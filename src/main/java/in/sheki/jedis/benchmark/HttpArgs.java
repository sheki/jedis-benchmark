package in.sheki.jedis.benchmark;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

import java.util.List;

/**
 * @author abhishekk
 */
public class HttpArgs
{
    @Parameter
    public List<String> parameters = Lists.newArrayList();

    @Parameter(names = "-n", description = "No. of apps.")
    public Integer noOps = 10000;

    @Parameter(names = "-t", description = "No. of Threads")
    public Integer noThreads = 0;

    @Parameter(names = "-u", description = "URL")
    public String url = "";
}
