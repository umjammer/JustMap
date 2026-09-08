package ru.bulldog.justmap.util.tasks;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import ru.bulldog.justmap.JustMap;

public class TaskManager implements Executor {
	private static final long SHUTDOWN_TIMEOUT = 5000;

	private final Queue<Task> workQueue = new ConcurrentLinkedQueue<>();
	private final QueueBlocker queueBlocker;
	private final ThreadGroup group;
	private final Thread[] workers;
	private String name = JustMap.MODID;

	private volatile boolean stopping = false;

	private static final Map<String, TaskManager> managers = new ConcurrentHashMap<>();

	public static TaskManager getManager(String name) {
		return getManager(name, 1);
	}

	public static synchronized TaskManager getManager(String name, int maxThreads) {
		TaskManager manager = managers.get(name);
		if (manager != null && manager.isRunning()) {
			return manager;
		}

		manager = new TaskManager(name, maxThreads);
		managers.put(name, manager);

		return manager;
	}

	public static void shutdown() {
		managers.values().forEach(TaskManager::stopAndWait);
	}

	private TaskManager(String name, int maxThreads) {
		this.name += "-" + name;
		this.queueBlocker = new QueueBlocker(this.name + "-blocker");
		this.workers = new Thread[maxThreads];
		this.group = new ThreadGroup(this.name);
		for (int i = 0; i < maxThreads; i++) {
			String threadName = String.format("%s-%d", this.name, i + 1);
			this.workers[i] = new Thread(group, this::work, threadName);
			// never keep the JVM alive: shutdown joins the workers, this is only a safety net
			this.workers[i].setDaemon(true);
			this.workers[i].start();
		}
	}

	public void execute(String reason, Runnable command) {
		this.workQueue.offer(new Task(reason, command));
		this.unpark();
	}

	private void unpark() {
		for (Thread worker : workers) {
			LockSupport.unpark(worker);
		}
	}

	@Override
	public void execute(Runnable command) {
		this.execute(null, command);
	}

	public <T> CompletableFuture<T> run(Function<CompletableFuture<T>, Runnable> function) {
		return this.run(null, function);
	}

	public <T> CompletableFuture<T> run(String reason, Function<CompletableFuture<T>, Runnable> function) {
		CompletableFuture<T> completableFuture = new CompletableFuture<>();
		this.execute(reason, function.apply(completableFuture));
		return completableFuture;
	}

	/**
	 * Asks every worker to leave as soon as the queue is drained. Doesn't wait.
	 */
	public void stop() {
		this.stopping = true;
		this.unpark();
	}

	private void stopAndWait() {
		if (!this.isRunning()) return;

		this.stop();
		long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT;
		for (Thread worker : workers) {
			long wait = deadline - System.currentTimeMillis();
			if (wait <= 0) break;
			try {
				worker.join(wait);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		if (this.isRunning()) {
			this.workQueue.clear();
			this.unpark();
			JustMap.LOGGER.warning(String.format("%s didn't stop in %d ms", this.name, SHUTDOWN_TIMEOUT));
		} else {
			JustMap.LOGGER.debug(this.name + " stopped");
		}
	}

	public int queueSize() {
		return this.workQueue.size();
	}

	public boolean isRunning() {
		for (Thread worker : workers) {
			if (worker.isAlive()) return true;
		}
		return false;
	}

	private void work() {
		while (true) {
			Task nextTask = workQueue.poll();
			if (nextTask != null) {
				if (nextTask.hasReason()) {
					JustMap.LOGGER.debug(nextTask);
				}
				try {
					nextTask.run();
				} catch (Throwable ex) {
					// a failing task must not take the worker down with it
					JustMap.LOGGER.error(String.format("Task failed in %s", this.name));
					JustMap.LOGGER.catching(ex);
				}
			} else if (stopping) {
				break;
			} else {
				LockSupport.park(queueBlocker);
			}
		}
	}

	private static class Task implements Runnable {

		private final Runnable task;
		private final String reason;

		private Task(String reason, Runnable task) {
			this.reason = reason;
			this.task = task;
		}

		public String getReason() {
			return this.reason;
		}

		public boolean hasReason() {
			return this.reason != null;
		}

		@Override
		public void run() {
			this.task.run();
		}

		@Override
		public String toString() {
			return this.getReason();
		}
	}

	private static class QueueBlocker {
		private final String name;

		private QueueBlocker(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
