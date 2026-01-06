package tech.kayys.wayang.api.node;

public interface CancellationToken {

    /**
     * @return true if workflow or node execution is requested to stop.
     */
    boolean isCancellationRequested();

    /**
     * @return true if the execution has been cancelled.
     */
    boolean isCancelled();

    /**
     * Register a callback invoked when cancellation occurs.
     * 
     * @param runnable callback executed on cancellation
     */
    void onCancelled(Runnable runnable);

    void throwIfCancellationRequested() throws InterruptedException;
}
