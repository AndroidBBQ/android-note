AsyncTask的源码分析：
构造方法：
```java
public abstract class AsyncTask<Params, Progress, Result> {
}
```
其中Params为参数类型，Progress为后台任务执行进度的类型，Result为返回结果的类型。如果不需要某个参数，可以将其设置为Void类型。

四个核心方法：
（1）onPreExecute（）：在主线程中执行。一般在任务执行前做准备工作，比如对 UI 做一些标记。
（2）doInBackground（Params...params）：在线程池中执行。在 onPreExecute方法执行后运行，用来执行较为耗时的操作。在执行过程中可以调用publishProgress（Progress...values）来更新进度信息。
（3）onProgressUpdate（Progress...values）：在主线程中执行。当调用
publishProgress（Progress...values）时，此方法会将进度更新到UI组件上。
（4）onPostExecute（Result  result）：在主线程中执行。当后台任务执行完成后，它会被执行。doInBackground方法得到的结果就是返回的result的值。此方法一般做任务执行后的收尾工作，比如更新UI
和数据。

源码分析：
1.在静态方法块中构建线程池
```java
//核心线程数 为2-4 
private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
//最大线程数 5-9
 private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
//线程闲置等待时间30s
 private static final int KEEP_ALIVE_SECONDS = 30;
//阻塞队列容量 128
 private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);
  public static final Executor THREAD_POOL_EXECUTOR;
//在静态方法中构造线程池
    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }
```
2.构造方法中做了什么：
```java
 public AsyncTask(@Nullable Looper callbackLooper) {
     //确保handler的loop是主线程的
        mHandler = callbackLooper == null || callbackLooper == Looper.getMainLooper()
            ? getMainHandler()
            : new Handler(callbackLooper);
        //看两个参数，Params, Result ，
       //WorkerRunnable 的父类有Params的变量  实现了Callback的接口
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Result result = null;
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    //调用了doInBackground并获取返回结果
                    result = doInBackground(mParams);
                    Binder.flushPendingCommands();
                } catch (Throwable tr) {
                    mCancelled.set(true);
                    throw tr;
                } finally {
                    //将结果交给了result
                    postResult(result);
                }
                return result;
            }
        };
        //这个FutureTask是一个可管理的异步任务，可以看到mWorker参数，被封装到了这个里面，它实现了RunnableFuture接口
        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }
```
3.接下来看执行方法：
```java
 public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }
```
可以看到调用了executeOnExecutor方法
```java
@MainThread
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
            Params... params) {
          //调用了onPreExecute方法，是在主线程中
        onPreExecute();
        //将params封装到了构造方法创建的mWorker中了
        mWorker.mParams = params;
       //执行mFuture(在构造方法中构建的，封装了mWorker)
        exec.execute(mFuture);
        return this;
    }
```
逻辑就是上面的，可以看到exec这个线程池中，执行的mFuture，这个exec是什么？从上面中可以看到sDefaultExecutor。
```java
  public static final Executor SERIAL_EXECUTOR = new SerialExecutor();
   private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
```
可以看到exec实际上是SerialExecutor：
```java
private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        //执行
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }
```
上面的逻辑并不复杂，可以看到，当任务执行完或者当前没有活动的任务时都会执行scheduleNext方法，它会从 mTasks 取出 FutureTask任务并交由 THREAD_POOL_EXECUTOR 处理。可以看出，AsyncTask内部处理任务是串行的。

上面可以看到FutereTask的run方法执行了，看一下其中内部的执行。
```java
   public void run() {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                    result = c.call();       
    }
```
可以看到，如果callable不为null则调用callable的call方法，这个callable在构造方法中已经知道是mWorker了，所以回到了mWorker的call方法中。
```java
 mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Result result = null;
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    //noinspection unchecked
                    result = doInBackground(mParams);
                    Binder.flushPendingCommands();
                } catch (Throwable tr) {
                    mCancelled.set(true);
                    throw tr;
                } finally {
                    postResult(result);
                }
                return result;
            }
        };
```
可以看到在执行完doInBackground后，将返回的result交给了postResult来执行：
```java
 private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT,
                new AsyncTaskResult<Result>(this, result));
        message.sendToTarget();
        return result;
    }
```
将结果通过handler发送到了handler中。
```java
 private static class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }
```
最终调用了mTask的finish方法
```java
private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }
```