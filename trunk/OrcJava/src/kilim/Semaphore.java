package kilim;

import java.util.LinkedList;

/**
 * Same as {@link java.util.concurrent.Semaphore}, but
 * for Kilim tasks.
 * @author quark
 */
public class Semaphore {
	/**
	 * Task waiting to acquire the semaphore.
	 */
	private static class Waiter implements PauseReason {
		private final Task task;
		private boolean valid = true;
		public Waiter(Task task) {
			this.task = task;
		}
		public boolean isValid() {
			return valid;
		}
		public void resume() {
			valid = false;
			task.resume();
		}
	}
	
	private LinkedList<Waiter> waiters = new LinkedList<Waiter>();
	private int n;
	public Semaphore(int n) {
		this.n = n;
	}
	public @pausable synchronized void acquire() {
		final Object uniq = new Object();
		while (n == 0) {
			Waiter w = new Waiter(Task.getCurrentTask());
			waiters.add(w);
	        Task.pause(w);
		}
		--n;
	}
	public void release() {
		Waiter w;
		synchronized (this) {
			++n;
			w = waiters.poll();
		}
		if (w != null) {
			w.resume();
		}
	}
}