/**
 * 
 */
package icblive.chatserver.utils;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

import io.dropwizard.metrics.Counter;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.Timer;
import io.dropwizard.metrics.graphite.Graphite;
import io.dropwizard.metrics.graphite.GraphiteReporter;

/**
 * @author jgao
 *
 */
public class ChangbaMetrics {

	private static final MetricRegistry metrics = new MetricRegistry();

	
	private static final String GRAPHITE_HOST = ChangbaConfig.getConfig().graphite_host;
	
    // 总用户在线统计
	public static final Counter userCount  = metrics.counter("online_user");
    // 唱吧用户在线统计
	public static final Counter cbUserCount  = metrics.counter("cb_online_user");
	public static final Counter roomCount  = metrics.counter("online_room");
	
	private static final Map<String, Meter> meterMap = Maps.newConcurrentMap();
	private static final Map<String, Timer> timerMap = Maps.newConcurrentMap();
	
	public static enum MeterName{
		
	}
	
	public static void incrMeter(String key){
		key = "meter."+key;
		if ( ! meterMap.containsKey(key)) {
			Meter meter = metrics.meter(key);
			meterMap.put(key, meter);
		}
		Meter meter = meterMap.get(key);
		meter.mark();
	}
	
	public static Meter getMeter(String key){
		key = "meter."+key;
		if ( ! meterMap.containsKey(key)) {
			return null;
		}
		Meter meter = meterMap.get(key);
		return meter;
	}
	
	public static Timer getTimer(String key){
		key = "timer."+key;
		if ( ! timerMap.containsKey(key)) {
			Timer timer = metrics.timer(key);
			timerMap.put(key, timer);
			return timer;
		}
		return timerMap.get(key);
	}
	
	
	
	public static void startReport() throws Exception {
//      ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
//      .convertRatesTo(TimeUnit.SECONDS)
//      .convertDurationsTo(TimeUnit.MILLISECONDS)
//      .build();
//  reporter.start(1, TimeUnit.SECONDS);
	  final Graphite graphite = new Graphite(new InetSocketAddress(GRAPHITE_HOST, 2003));
	  final GraphiteReporter reporter = GraphiteReporter.forRegistry(metrics)
	                                                    .prefixedWith("marsws." + ChangbaConfig.getConfig().ws_server_name + "_" + ChangbaConfig.getConfig().ws_port)
	                                                    .convertRatesTo(TimeUnit.SECONDS)
	                                                    .convertDurationsTo(TimeUnit.MILLISECONDS)
	                                                    .filter(MetricFilter.ALL)
	                                                    .build(graphite);
	  reporter.start(15, TimeUnit.SECONDS);
	}
}
