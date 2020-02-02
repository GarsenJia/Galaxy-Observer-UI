package interfacebuilder.threads;

import interfacebuilder.InterfaceBuilderApp;
import interfacebuilder.integration.log4j.StylizedTextAreaAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ForkJoinTask;

/**
 * A task for the ForkJoinPool that allows InterfaceBuilderApp to clean up after itself, if execAndClean() is called
 * instead of the usual exec().
 * <p>
 * exec() should still be overridden with the actual workload.
 */
public abstract class CleaningForkJoinTask extends ForkJoinTask<Void> {
	private static final Logger logger = LogManager.getLogger(CleaningForkJoinTask.class);
	
	// TODO maybe make this more generic, so this can be re-used -> define interface & call member of that to clean up
	
	@Override
	public Void getRawResult() {
		return null;
	}
	
	@Override
	protected void setRawResult(final Void value) {
	}
	
	/**
	 * Executes the task and causes the InterfacebuilderApp to free up resources.
	 *
	 * @return
	 */
	@Override
	protected boolean exec() {
		try {
			return work();
		} catch (final Exception e) {
			logger.error("Error in Task:", e);
			return false;
		} finally {
			StylizedTextAreaAppender.finishedWork(Thread.currentThread().getName(), true);
			InterfaceBuilderApp.tryCleanUp();
		}
	}
	
	/**
	 * CleaningForkJoinTask's workload that is executed.
	 *
	 * @return true, if the task finished normally
	 */
	protected abstract boolean work();
}