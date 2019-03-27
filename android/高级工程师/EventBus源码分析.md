# 基本使用
不管是Activity和Activity，Activity和Fragment等等之间的通信写法都差不多。这里以在Activity中在子线程中发送Msg，在主线程中接收为例，
## 1.添加依赖
build.gradle 配置如下
```
compile 'org.greenrobot:eventbus:3.0.0'
```

## 2.定义Event事件
定义要传递的对象，当然如果要传递的是普通类型，可以不定义。这里我们就让传递的Event类型是自定义的Msg对象
```
public class Msg {
    public String text; //发送的文本
    public String from; //发送者
}
```
## 3.注册事件
在Activity中可以在onStart中调用这个注册方法，在Fragment中直接调用`EventBus.getDefault().register(this)`即可实现注册
```
@Override
protected void onStart() {
    super.onStart();
    //注册事件
    EventBus.getDefault().register(this);
}
```
## 4.解除注册
注意由于EventBus使用的是单例模式，在注册后在对象销毁后一定要解除注册，否则可能引起**内存泄漏**。
```java
 @Override
 protected void onDestroy() {
     EventBus.getDefault().unregister(this);
     super.onDestroy();
 }
```

## 5.发送消息
发送消息可以在任意场合发送，比如在Activity,Fragment，service等中都可以，直接调用getDefault获取到EventBus对象，然后调用post方法将Event事件对象发送就行。
```java
  Msg msg = new Msg();
  msg.from = "子线程";
  msg.text ="hello";
  EventBus.getDefault().post(msg);
```
## 6.接收消息
在注册的对象中任意定义方法，注意**这个方法类型必须是public**，然后**在方法上添加@Subscribe注解**，**必须只有一个参数，参数类型必须是Event事件对象**，
```java
  @Subscribe(threadMode = ThreadMode.MAIN,priority = 100,sticky = true)
    public void recice(Msg msg) {
        Log.i("TAG", "收到消息:消息发送者:"+msg.from+"消息文本:"+msg.text);
    }
```
对应接收消息的@Subscribe注解中，三个参数
### ThreadMode
*   **POSTING (默认):** 表示事件处理函数的线程跟发布事件的线程在同一个线程。
*   **MAIN:** 表示事件处理函数的线程在主线程(UI)线程，因此在这里不能进行耗时操作。
*   **BACKGROUND:** 表示事件处理函数的线程在后台线程，因此不能进行UI操作。如果发布事件的线程是主线程(UI线程)，那么事件处理函数将会开启一个后台线程，如果果发布事件的线程是在后台线程，那么事件处理函数就使用该线程。
*   **ASYNC:** 表示无论事件发布的线程是哪一个，事件处理函数始终会新建一个子线程运行，同样不能进行UI操作。

### priority
优先级，可以设置这个属性，优先级越高，就越先执行。

# 源码分析

## 1.获取Eventbus的实例
```java
 public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }
```
采用双重校验并加锁的单例模式生成 EventBus 实例
## 2.register(this)
```java
public void register(Object subscriber) {
		//获取订阅者的class对象
        Class<?> subscriberClass = subscriber.getClass();
        //从订阅者class中找到到订阅方法
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
            	//对每个方法进行订阅
                subscribe(subscriber, subscriberMethod);
            }
        }
    }
```
找出定义类中的所有订阅方法

### 2.1 SubscriberMethodFinder#findSubscriberMethods()
```java
 List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
 		//如果缓存中有对应 class 的订阅方法列表，则直接返回，这里我们是第一次创建，所以此时 subscriberMethods 为空；
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
		//当缓存中没有找到该观察者的订阅方法的时候使用下面的两种方法获取方法信息
        if (ignoreGeneratedIndex) {//默认false
            subscriberMethods = findUsingReflection(subscriberClass);
        } else {
            subscriberMethods = findUsingInfo(subscriberClass);
        }
		//在订阅的类中必须要有订阅的方法，否则会抛异常
         if (subscriberMethods.isEmpty()) {
				throw new EventBusException("Subscriber " + subscriberClass + " and its super classes have no public methods with the @Subscribe annotation");
    	} else {
				METHOD_CACHE.put(subscriberClass, subscriberMethods);
				return subscriberMethods;
    	}
    }
```

上面的主要逻辑在 findUsingInfo 方法中
```java
private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
		//1.这里通过FindState对象来存储找到的方法信息
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        //这里是一个循环操作，会从当前类开始遍历该类的所有父类
        while (findState.clazz != null) {
        	//2.获取订阅者信息  第一次获取的是null
            findState.subscriberInfo = getSubscriberInfo(findState);
            if (findState.subscriberInfo != null) {
            	//如果使用了MyEventBusIndex，将会进入到这里并获取订阅方法信息
                SubscriberMethod[] array = findState.subscriberInfo
                .getSubscriberMethods();
                for (SubscriberMethod subscriberMethod : array) {
                    if (findState.checkAdd(subscriberMethod.method,
                    		subscriberMethod.eventType)) {
                        findState.subscriberMethods.add(subscriberMethod);
                    }
                }
            } else {
            	//3 使用反射获取方法信息
                findUsingReflectionInSingleClass(findState);
            }
            //将findState.clazz设置为当前的findState.clazz的父类
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }
```
**第一步**
SubscriberMethodFinder#prepareFindState()
```java
//创建一个新的 FindState 类，通过两种方法获取
private FindState prepareFindState() {
	//如果从FIND_STATE_POOL能够取出FindState 则将取出的返回
    synchronized (FIND_STATE_POOL) {
        for (int i = 0; i < POOL_SIZE; i++) {
            FindState state = FIND_STATE_POOL[i];
            if (state != null) {
                FIND_STATE_POOL[i] = null;
                return state;
            }
        }
    }
    //直接new出一个
    return new FindState();
}
```
FindState#initForSubscriber方法
```java
//做些初始化的操作
void initForSubscriber(Class<?> subscriberClass) {
    this.subscriberClass = clazz = subscriberClass;
    skipSuperClasses = false;
    subscriberInfo = null;//订阅信息默认是null
}
```
**第二步**
```java

private SubscriberInfo getSubscriberInfo(FindState findState) {
	//从第一步的最后可以看到 subscriberInfo 是null
    if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
        SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
        if (findState.clazz == superclassInfo.getSubscriberClass()) {
            return superclassInfo;
        }
    }
    //默认也是空
    if (subscriberInfoIndexes != null) {
        for (SubscriberInfoIndex index : subscriberInfoIndexes) {
            SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
            if (info != null) {
                return info;
            }
        }
    }
    //默认返回为空
    return null;
    }
```
** 第三步**
通过反射获取方法信息
```java
 private void findUsingReflectionInSingleClass(FindState findState) {
 		//获取类中的所有方法
        Method[] methods;
        methods = findState.clazz.getDeclaredMethods();
        //遍历所有方法
        for (Method method : methods) {
        	//获取方法的修饰 并判断 public
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
            	//获取方法的参数
                Class<?>[] parameterTypes = method.getParameterTypes();
                //方法参数必须一个的
                if (parameterTypes.length == 1) {
                	//获取方法上是 Subscribe 的注解的方法
                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation != null) {
                    	//获取该方法的第一个参数，也只有一个参数
                        Class<?> eventType = parameterTypes[0];
                        //检查方法和参数类型
                        if (findState.checkAdd(method, eventType)) {
                        	//获取注解中的线程model
                            ThreadMode threadMode = subscribeAnnotation.threadMode();
                            //将信息封装到 SubscriberMethod 中，并添加到 FindState 的 subscriberMethods 中
                            findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                        }
                    }
          //下面是对相关错误进行处理
                } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException("@Subscribe method " + methodName +
                            "must have exactly 1 parameter but has " + parameterTypes.length);
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException(methodName +
                        " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
            }
        }
    }
```

### 2.2 subscribe(subscriber, subscriberMethod)
```java
 // Must be called in synchronized block
 //第一个参数是 订阅的类 第二个参数是 订阅中符合eventbus 规则的订阅方法
 private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
     Class<?> eventType = subscriberMethod.eventType;
     //将 订阅的类 和 订阅方法 封装成 Subscription
     Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
     
     //线程安全的数组型的数据结构 key是事件类型 value是Subscription
     // Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
     CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
     if (subscriptions == null) {
         subscriptions = new CopyOnWriteArrayList<>();
         //主要用来存储一个事件类型所对应的全部的Subscription对象
         subscriptionsByEventType.put(eventType, subscriptions);
     } else {
         if (subscriptions.contains(newSubscription)) {
             throw new EventBusException("...);
         }
     }
     
	//这里会根据新加入的方法的优先级决定插入到队列中的位置
     int size = subscriptions.size();
     for (int i = 0; i <= size; i++) {
         if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
             subscriptions.add(i, newSubscription);
             break;
         }
     }
     
	 // 这里会从“订阅者-事件类型”列表中尝试获取该订阅者对应的所有事件类型
     // Map<Object, List<Class<?>>> typesBySubscriber;
     List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
     if (subscribedEvents == null) {
         subscribedEvents = new ArrayList<>();
         //key : 订阅者 value: 事件类型集合
         typesBySubscriber.put(subscriber, subscribedEvents);
     }
     //添加订阅者中的的 事件类型
     subscribedEvents.add(eventType);

	//如果是粘性事件，将当前缓存中的信息发送给新订阅的方法
     if (subscriberMethod.sticky) {
         if (eventInheritance) {
             Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
             for (Map.Entry<Class<?>, Object> entry : entries) {
                 Class<?> candidateEventType = entry.getKey();
                 if (eventType.isAssignableFrom(candidateEventType)) {
                     Object stickyEvent = entry.getValue();
                     //将当前缓存中的信息发送给新订阅的方法和直接调用post方法差不多。
                     checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                 }
             }
         } else {
             Object stickyEvent = stickyEvents.get(eventType);
             checkPostStickyEventToSubscription(newSubscription, stickyEvent);
         }
     }
 }
```
上面的代码中一些逻辑都说的很清楚了，对上面的代码进行总结一下。上面的方法中主要做了三个操作。第一：在subscriptionsByEventType 中封装以 事件类型为 key ，以 Subscription(封装了订阅者和相关的订阅方法) 集合为 value 。第二：在 typesBySubscriber 中封装以 订阅者为 key ，事件类型 集合为value。第三：如果是粘性事件，将当前缓存中的消息发送给新的订阅方法。


## 3.EventBus#post方法
```java
public void post(Object event) {
	// 这里从线程局部变量中取出当前线程的状态信息
    PostingThreadState postingState = currentPostingThreadState.get();//1.
    // 这里是以上线程局部变量内部维护的一个事件队列
    List<Object> eventQueue = postingState.eventQueue;
    //将发送的事件添加到了事件队列中了
    eventQueue.add(event);
	
    //默认为false
    if (!postingState.isPosting) {
    	//给postingState中的状态赋值
        postingState.isMainThread = isMainThread();
        postingState.isPosting = true;//正在分发事件
        if (postingState.canceled) {//默认为false
            throw new EventBusException("Internal error. Abort state was not reset");
        }
        try {
        	//核心，不断来分发事件。
            while (!eventQueue.isEmpty()) {
            	//传递的有，先将队列中的 event 移除，然后将event作为参数传递，postingState
                postSingleEvent(eventQueue.remove(0), postingState);
            }
        } finally {
            postingState.isPosting = false;//将正在分发状态设置为false
            postingState.isMainThread = false;//
        }
    }
}
```
上面的代码 1 处的 currentPostingThreadState 其实是ThreadLocal，保存了线程相关的PostingThreadState
```java
private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };
 final static class PostingThreadState {
        final List<Object> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        Object event;
        boolean canceled;
    }
```

post方法中，做的就是将要发送的事件添加到使用ThreadLocal维持的 eventQueue 中，最后通过 postSingleEvent 方法一个个的将事件分发出去。

postSingleEvent方法：
```java
 private void postSingleEvent(Object event, PostingThreadState postingState){
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;
        //这里只关心重点 是通过这个方法分发事件的
        subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
    }
```
postSingleEventForEventType方法：
```java
private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
	//这一步熟悉不？ 通过事件类型 获取 Subscription 的集合，看上面的2 中的第三步
    CopyOnWriteArrayList<Subscription> subscriptions;
    synchronized (this) {
        subscriptions = subscriptionsByEventType.get(eventClass);
    }
    //如果可以获取到Subscription
    if (subscriptions != null && !subscriptions.isEmpty()) {
        for (Subscription subscription : subscriptions) {//遍历Subscription
        	//又把相关信息封装到了 postingState 了
            postingState.event = event;
            postingState.subscription = subscription;
            boolean aborted = false;
            try {
            	//核心分发事件
                postToSubscription(subscription, event, postingState.isMainThread);
                aborted = postingState.canceled;
            } finally {
            	//分发完事件后，做了相关处理
                postingState.event = null;
                postingState.subscription = null;
                postingState.canceled = false;
            }
            if (aborted) {
                break;
            }
        }
        return true;
    }
    return false;
}
```
postToSubscription方法

四种线程模式：
* POSTING (默认)  表示事件处理函数的线程跟发布事件的线程在同一个线程。
* MAIN 表示事件处理函数的线程在主线程(UI)线程，因此在这里不能进行耗时操作。
* BACKGROUND 表示事件处理函数的线程在后台线程，因此不能进行UI操作。如果发布事件的线程是主线程(UI线程)，那么事件处理函数将会开启一个后台线程，如果果发布事件的线程是在后台线程，那么事件处理函数就使用该线程。
* ASYNC 表示无论事件发布的线程是哪一个，事件处理函数始终会新建一个子线程运行，同样不能进行UI操作
* MAIN_ORDERED 事件处理函数在ui线程，事件总是先入队，后交付给用户。事件处理严格按串行顺序


```java
private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
	//根据不同的线程模式，做不同的处理
    switch (subscription.subscriberMethod.threadMode) {
        case POSTING://直接处理
            invokeSubscriber(subscription, event);
            break;
        case MAIN:
            if (isMainThread) {//如果是主线程
                invokeSubscriber(subscription, event);
            } else {//否则加入mainThreadPoster 中进行处理
                mainThreadPoster.enqueue(subscription, event);
            }
            break;
        case MAIN_ORDERED:
            if (mainThreadPoster != null) {
                mainThreadPoster.enqueue(subscription, event);
            } else {
                invokeSubscriber(subscription, event);
            }
            break;
        case BACKGROUND:
            if (isMainThread) {
                backgroundPoster.enqueue(subscription, event);
            } else {
                invokeSubscriber(subscription, event);
            }
            break;
        case ASYNC:
            asyncPoster.enqueue(subscription, event);
            break;
        default:
            throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
    }
}
```
上面根据情况的不同，可以分为四种处理：
1. invokeSubscriber
2. mainThreadPoster.enqueue
3. backgroundPoster.enqueue
4. asyncPoster.enqueue

(1) invokeSubscriber
```java
void invokeSubscriber(Subscription subscription, Object event) {
	//直接通过反射执行方法
   subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
 }
```
可以看到 invokeSubscriber 方法非常简单就是通过反射直接调用方法执行

(2) mainThreadPoster.enqueue

mainThreadPoster其实获取的是 `HandlerPoster`,为什么？代码如下：
```java
//在 EventBus 的构造方法中可以知道 mainThreadPoster 是调用了 MainThreadSupport 的 createPoster 方法
MainThreadSupport mainThreadSupport;

mainThreadPoster = mainThreadSupport.createPoster(this);

MainThreadSupport#createPoster ：
@Override
public Poster createPoster(EventBus eventBus) {
    return new HandlerPoster(eventBus, looper, 10);
}
```

mainThreadPoster 的 enqueue 方法实际上调用的是 HandlerPoster 的 enque 方法。
```java
 public void enqueue(Subscription subscription, Object event) {
 //将  Subscription 和 事件类型 封装到了 PendingPost 中了
  PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
  synchronized (this) {
  	  //将 pendingPost 添加到了队列中
      queue.enqueue(pendingPost);
      //如果是第一次添加post到队列中，发送一条消息给Handler
      if (!handlerActive) {//默认为false
          handlerActive = true;//改变标志位
          if (!sendMessage(obtainMessage())) {
              throw new EventBusException("Could not send handler message");
          }
      }
  }
}
```
在 HandlerPoster 的 enque 方法中 subscription 和 event 被封装到了 PendingPost 中，并添加到了queue中了，如果是第一次将 post 添加到 queue 中，发送一条消息到handler中，会调用 handler 的 handlerMessage 方法。

HandlerPoster 继承 Handler 重写 handleMessage 方法。
```java
@Override
public void handleMessage(Message msg) {
    boolean rescheduled = false;
    try {
    	//死循环
        while (true) {
        	//不停的从队列中取出 post
            PendingPost pendingPost = queue.poll();
            if (pendingPost == null) {
                synchronized (this) {
                    //如果 pendingPost 获取的是空，再取出一个 
                    pendingPost = queue.poll();
                    if (pendingPost == null) {
                        handlerActive = false;
                        return;
                    }
                }
            }
            //调用 eventbus 中的 invokeSubscriber 方法执行
            eventBus.invokeSubscriber(pendingPost);
        }
    }
}
```
在 handleMessage 方法中，开启了死循环，不断的从 eneque 中获取到封装的 PendingPost ，并调用 Eventbus 的方法执行这个方法。这样在 eneque 中只要加入了 PendingPost 中，就能立即被取出执行。

(3) backgroundPoster#eneque
按照上面的逻辑，backgroundPoster 其实是 BackgroundPoster 

BackgroundPoster#eneque
```java
public void enqueue(Subscription subscription, Object event) {
	//将  Subscription 和 事件类型 封装到了 PendingPost 中了
    PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
    synchronized (this) {
    	//添加到队列中
        queue.enqueue(pendingPost);
        if (!executorRunning) {//确保一次只执行一个
            executorRunning = true;
            //调用了 eventBus.getExecutorService() 获取到线程池，来执行当前的线程
            eventBus.getExecutorService().execute(this);
        }
    }
}
```
先来看看 `eventBus.getExecutorService()` 获取的是什么线程池
```java
 public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}
```
可以看到这个线程池和 `CacheThreadPool` 是一样的，由于 BackgroundPoster 实现了 Runnable 所以会调用 BackgroundPoster 的 run 方法。这里要知道会新建开一个线程来执行 run 方法。

BackgroundPoster#run
```java
@Override
public void run() {
	//同样是死循环
     while (true) {
     	//从队列中取出
         PendingPost pendingPost = queue.poll(1000);
         if (pendingPost == null) {
             synchronized (this) {
                 // Check again, this time in synchronized
                 pendingPost = queue.poll();
                 if (pendingPost == null) {
                     executorRunning = false;
                     return;
                 }
             }
         }
         //调用 eventbus 的方法执行这个方法
         eventBus.invokeSubscriber(pendingPost);
     }
 } 
}
```

(4) asyncPoster.enqueue
asyncPoster 其实是 AsyncPoster
```java
public void enqueue(Subscription subscription, Object event) {
     PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
     queue.enqueue(pendingPost);
     eventBus.getExecutorService().execute(this);
 }
```
可以看到和上面的 backgroundPoster 执行的过程一样。
