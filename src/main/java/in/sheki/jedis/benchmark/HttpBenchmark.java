package in.sheki.jedis.benchmark;

import com.beust.jcommander.JCommander;
import com.ning.http.client.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author abhishekk
 */
public class HttpBenchmark
{
    private final int noOps_;
    private final LinkedBlockingQueue<Long> setRunTimes = new LinkedBlockingQueue<Long>();
    private PausableThreadPoolExecutor executor;
    private final String url_;
    private final CountDownLatch shutDownLatch;
    private long totalNanoRunTime;
    private AtomicInteger failures = new AtomicInteger(0);

    //TODO fix this
    private static AsyncHttpClient client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setConnectionTimeoutInMs(100).setRequestTimeoutInMs(150).build());

    public HttpBenchmark(final int noOps, final int noThreads, final String url)
    {
        this.noOps_ = noOps;
        this.executor = new PausableThreadPoolExecutor(noThreads, noThreads, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        shutDownLatch = new CountDownLatch(noOps);
        this.url_ = url;
    }

    class AsyncHttpTask implements Runnable
    {
        AsyncHttpTask(CountDownLatch latch_)
        {
            this.latch_ = latch_;
        }

        private CountDownLatch latch_;


        public void run()
        {
            long starTime = System.nanoTime();
            try
            {
                client.preparePost(url_).addParameter("data", new String(Base64.encodeBase64(RandomStringUtils.random(100).getBytes()))).addParameter("type", "STRING").addParameter("timestamp", Long.toString(System.currentTimeMillis())).execute(new AsyncHandler<Boolean>()
                {
                    long start = System.nanoTime();

                    public void onThrowable(Throwable t)
                    {
                        failures.incrementAndGet();
                        t.printStackTrace();
                        latch_.countDown();
                    }

                    public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception
                    {
                        return STATE.CONTINUE;
                    }

                    public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception
                    {
                        return STATE.CONTINUE;
                    }

                    public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception
                    {
                        return STATE.CONTINUE;
                    }

                    public Boolean onCompleted() throws Exception
                    {
                        setRunTimes.offer(System.nanoTime() - start);
                        latch_.countDown();
                        return true;
                    }
                });
            }
            catch (IOException e)
            {
                e.printStackTrace();
                failures.incrementAndGet();
            }

        }
    }

    class HttpTask implements Runnable
    {
        private CountDownLatch latch_;

        HttpTask(CountDownLatch latch)
        {
            this.latch_ = latch;
        }

        public void run()
        {
            long starTime = System.nanoTime();
            boolean result;
            try
            {
                HttpPost httpost = new HttpPost(url_);
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, 100);
                HttpConnectionParams.setSoTimeout(httpParams, 150);
                HttpClient httpClient = new DefaultHttpClient(httpParams);
                byte[] dataArray = RandomStringUtils.random(100).getBytes();
                byte[] encodedBytes = Base64.encodeBase64(dataArray);
                nvps.add(new BasicNameValuePair("data", new String(encodedBytes)));
                nvps.add(new BasicNameValuePair("type", "STRING"));
                nvps.add(new BasicNameValuePair("timestamp", Long.toString(System.currentTimeMillis())));
                httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                HttpResponse response = httpClient.execute(httpost);
                StatusLine status = response.getStatusLine();
            }
            catch (Exception e)
            {
                failures.incrementAndGet();
            }
            setRunTimes.offer(System.nanoTime() - starTime);
            latch_.countDown();
        }
    }

    public void performBenchmark() throws InterruptedException
    {
        executor.pause();
        for (int i = 0; i < noOps_; i++)
        {
            executor.execute(new AsyncHttpTask(shutDownLatch));
        }
        long startTime = System.nanoTime();
        executor.resume();
        executor.shutdown();
        System.out.println("TEST STARTED");
        shutDownLatch.await();
        totalNanoRunTime = System.nanoTime() - startTime;
    }

    public void printStats()
    {
        List<Long> points = new ArrayList<Long>();
        setRunTimes.drainTo(points);
        Collections.sort(points);
        long sum = 0;
        for (Long l : points)
        {
            sum += l;
        }
        System.out.println("Data size :" + 100);
        System.out.println("No. of transactions " + points.size());
        System.out.println("Threads : " + executor.getMaximumPoolSize());
        System.out.println("Time Test Ran for (ms) : " + TimeUnit.NANOSECONDS.toMillis(totalNanoRunTime));
        System.out.println("Average : " + TimeUnit.NANOSECONDS.toMillis(sum / points.size()));
        System.out.println("50 % <=" + TimeUnit.NANOSECONDS.toMillis(points.get((points.size() / 2) - 1)));
        Sys`tem.out.println("90 % <=" + TimeUnit.NANOSECONDS.toMillis(points.get((points.size() * 90 / 100) - 1)));
        System.out.println("95 % <=" + TimeUnit.NANOSECONDS.toMillis(points.get((points.size() * 95 / 100) - 1)));
        System.out.println("99 % <=" + TimeUnit.NANOSECONDS.toMillis(points.get((points.size() * 99 / 100) - 1)));
        System.out.println("99.9 % <=" + TimeUnit.NANOSECONDS.toMillis(points.get((points.size() * 999 / 1000) - 1)));
        System.out.println("100 % <=" + TimeUnit.NANOSECONDS.toMillis(points.get(points.size() - 1)));
        System.out.println((noOps_ * 1000 / TimeUnit.NANOSECONDS.toMillis(totalNanoRunTime)) + " Operations per second");
        System.out.println("Failures :" + failures.get());
    }


    public static void main(String[] args) throws InterruptedException
    {
        HttpArgs cla = new HttpArgs();
        new JCommander(cla, args);
        HttpBenchmark benchmark = new HttpBenchmark(cla.noOps, cla.noThreads, cla.url);
        benchmark.performBenchmark();
        benchmark.printStats();
        client.close();
    }
}
