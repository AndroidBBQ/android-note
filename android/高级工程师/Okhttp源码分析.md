## 使用
### 基本的使用：
```java
// 1、创建 Request
Request request = new Request.Builder()
    .get()
    .url("xxx")
    .build(); 

// 2、创建 OKHttpClient
OkHttpClient client = new OkHttpClient();

// 3、创建 Call
Call call = client.newCall(request);

try {
    // 4、同步请求
    Response response = call.execute();
} catch (IOException e) {
    e.printStackTrace();
}

// 5、异步请求
call.enqueue(new Callback() {
    @Override
    public void onFailure(Call call, IOException e) {

    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {

    }
});
```
首先构建一个请求 Request 和一个客户端 OkHttpClient，然后 OkHttpClient 对象根据 request 调用 newCall 方法创建 Call 对象，再调用  execute 或者 enqueue 方法进行同步或者异步请求。


**具体的使用暂时不说，添加一些拦截器。**
参考链接：
[观战Retrofit开发中的哪点事](https://www.jianshu.com/p/07794cb4972a)
[Retrofit实现持久化Cookie的三种方案](https://www.jianshu.com/p/fcccf5907bab)

### 设置通用Header
```java
 public static Interceptor getRequestHeader() {
        Interceptor headerInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request.Builder builder = originalRequest.newBuilder();
                builder.header("appid", "1");
                builder.header("timestamp", System.currentTimeMillis() + "");
                builder.header("appkey", "zRc9bBpQvZYmpqkwOo");
                builder.header("signature", "dsljdljflajsnxdsd");
                Request.Builder requestBuilder = builder.method(originalRequest.method(), originalRequest.body());
                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        };
        return headerInterceptor;
    }
```
### 统一输出请求日志
添加依赖
```java
compile 'com.squareup.okhttp3:logging-interceptor:3.1.2'
```
代码
```java
 public static HttpLoggingInterceptor getHttpLoggingInterceptor() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return loggingInterceptor;
    }

```
### 拦截服务器响应
比如，从响应中获取time参数
```java
  public static Interceptor getResponseHeader () {
            Interceptor interceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response response = chain.proceed(chain.request());
                    String timestamp = response.header("time");
                    if (timestamp != null) {
                        //获取到响应header中的time 
                    }
                    return response;
                }
            };
            return interceptor;
        }
```

### 设置通用请求参数
```java
 Interceptor commonParams = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request originRequest = chain.request();
                Request request;
                //String method = originRequest.method();
                //Headers headers = originRequest.headers();
                HttpUrl httpUrl = originRequest.url().newBuilder()
                        .addQueryParameter("paltform", "android")
                        .addQueryParameter("version", "1.0.0")
                        .build();
                request = originRequest.newBuilder().url(httpUrl).build();
                return chain.proceed(request);
            }
        }
```
### 失败重连
```
 public void setRetry(OkHttpClient.Builder builder) {
        builder.retryOnConnectionFailure(true);
    }
```
### 请求超时
```
 public void setConnecTimeout (OkHttpClient.Builder builder){
            builder.connectTimeout(10, TimeUnit.SECONDS);
            builder.readTimeout(20, TimeUnit.SECONDS);
            builder.writeTimeout(20, TimeUnit.SECONDS);
        }
```
### 添加缓存，这个还不太懂，这里先记两种最容易的非持久化缓存和持久化缓存，后面有时间再弄清原理。

自定义拦截器实习的缓存。

首先定义响应拦截器，该拦截器实现从response获取set-cookie字段的值，并将其保存在本地。
```java
public class SaveCookiesInterceptor implements Interceptor {
    private static final String COOKIE_PREF = "cookies_prefs";
    private Context mContext;

    public SaveCookiesInterceptor(Context context) {
        mContext = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        //set-cookie可能为多个
        if (!response.headers("set-cookie").isEmpty()) {
            List<String> cookies = response.headers("set-cookie");
            String cookie = encodeCookie(cookies);
            saveCookie(request.url().toString(),request.url().host(),cookie);
        }

        return response;
    }

    //整合cookie为唯一字符串
    private String encodeCookie(List<String> cookies) {
        StringBuilder sb = new StringBuilder();
        Set<String> set=new HashSet<>();
        for (String cookie : cookies) {
            String[] arr = cookie.split(";");
            for (String s : arr) {
                if(set.contains(s))continue;
                set.add(s);

            }
        }

        Iterator<String> ite = set.iterator();
        while (ite.hasNext()) {
            String cookie = ite.next();
            sb.append(cookie).append(";");
        }

        int last = sb.lastIndexOf(";");
        if (sb.length() - 1 == last) {
            sb.deleteCharAt(last);
        }

        return sb.toString();
    }

    //保存cookie到本地，这里我们分别为该url和host设置相同的cookie，其中host可选
    //这样能使得该cookie的应用范围更广
    private void saveCookie(String url,String domain,String cookies) {
        SharedPreferences sp = mContext.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (TextUtils.isEmpty(url)) {
            throw new NullPointerException("url is null.");
        }else{
            editor.putString(url, cookies);
        }

        if (!TextUtils.isEmpty(domain)) {
            editor.putString(domain, cookies);
        }

        editor.apply();

    }
}

```

其次定义请求拦截器，如果该请求存在cookie，则为其添加到Header的Cookie中，代码如下：
```java
public class AddCookiesInterceptor implements Interceptor {
    private static final String COOKIE_PREF = "cookies_prefs";
    private Context mContext;

    public AddCookiesInterceptor(Context context) {
        mContext = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();
        String cookie = getCookie(request.url().toString(), request.url().host());
        if (!TextUtils.isEmpty(cookie)) {
            builder.addHeader("Cookie", cookie);
        }

        return chain.proceed(builder.build());
    }

    private String getCookie(String url, String domain) {
        SharedPreferences sp = mContext.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE);
        if (!TextUtils.isEmpty(url)&&sp.contains(url)&&!TextUtils.isEmpty(sp.getString(url,""))) {
            return sp.getString(url, "");
        }
        if (!TextUtils.isEmpty(domain)&&sp.contains(domain) && !TextUtils.isEmpty(sp.getString(domain, ""))) {
            return sp.getString(domain, "");
        }

        return null;
    }
}
```

## 源码分析

### 1.构建Request
```java
public class Request{
	//将buider的参数转移到Request中
	Request(Builder builder) {
    	this.url = builder.url;
    	this.method = builder.method;
    	this.headers = builder.headers.build();
    	this.body = builder.body;
    	this.tags = Util.immutableMap(builder.tags);
  	}
	//通过Builder来构建相关的Request
	public static class Builder {
    	//默认的请求方式get
    	public Builder() {
      		this.method = "GET";
      		this.headers = new Headers.Builder();
    	}
        //就是将url封装成httpurl并作为参数存放在Builder中
        public Builder url(String url) {
			 return url(HttpUrl.get(url));
   		 }
         public Builder url(HttpUrl url) {
     		 this.url = url;
      		return this;
    	}
        //其他的放到都是调用了method方法
        public Builder post(RequestBody body) {
      		return method("POST", body);
    	}
        //这个方法中把method和body都作为参数存在builder中
        public Builder method(String method, @Nullable RequestBody body) {
      		this.method = method;
     		 this.body = body;
      		return this;
    	}
        //通过buidler构造Request
        public Request build() {
     		 return new Request(this);
    	}
    }
}
```
### 2.OkhttpClient
通过okhttpClient的Builder来设置参数，同样我们这里忽略大部分细节地方，只关注核心
```java
public Builder() {
	//分发器
      dispatcher = new Dispatcher();
      protocols = DEFAULT_PROTOCOLS;
      connectionSpecs = DEFAULT_CONNECTION_SPECS;
      //事件监听工程
      eventListenerFactory = EventListener.factory(EventListener.NONE);
      proxySelector = ProxySelector.getDefault();
      cookieJar = CookieJar.NO_COOKIES;
      socketFactory = SocketFactory.getDefault();
      hostnameVerifier = OkHostnameVerifier.INSTANCE;
      certificatePinner = CertificatePinner.DEFAULT;
      proxyAuthenticator = Authenticator.NONE;
      authenticator = Authenticator.NONE;
      connectionPool = new ConnectionPool();
      dns = Dns.SYSTEM;
      followSslRedirects = true;
      followRedirects = true;
      retryOnConnectionFailure = true;
      //默认的连接实际，读写时间
      connectTimeout = 10_000;
      readTimeout = 10_000;
      writeTimeout = 10_000;
      pingInterval = 0;
    }
//设置连接时间，读写时间
public Builder connectTimeout(long timeout, TimeUnit unit) {
      connectTimeout = checkDuration("timeout", timeout, unit);
      return this;
    }
public Builder readTimeout(long timeout, TimeUnit unit) {
      readTimeout = checkDuration("timeout", timeout, unit);
      return this;
    }
public Builder writeTimeout(long timeout, TimeUnit unit) {
      writeTimeout = checkDuration("timeout", timeout, unit);
      return this;
    }
//设置ssl
public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
      this.sslSocketFactory = sslSocketFactory;
      this.certificateChainCleaner = Platform.get().buildCertificateChainCleaner(sslSocketFactory);
      return this;
    }
//添加拦截器
public Builder addInterceptor(Interceptor interceptor) {
      interceptors.add(interceptor);
      return this;
}
//添加网络拦截器
public Builder addNetworkInterceptor(Interceptor interceptor) {
      networkInterceptors.add(interceptor);
      return this;
}
//构造OkHttpClient
public OkHttpClient build() {
      return new OkHttpClient(this);
    }
```
通过上面的OkhttpClien的Builder的源码可以看出，里面还是很平常，在Builder中设置些默认的参数，然后设置一些参数都是放到了Builder中了，添加的拦截器和网络拦截器被添加到了相应的集合中。

### 3.生成Call
```java
public Call newCall(Request request) {
    return RealCall.newRealCall(this, request, false /* for web socket */);
  }
```
实际上调用的是RealCall的newRealCall方法，实际上返回的是RealCall。
```java
static RealCall newRealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
    // Safely publish the Call instance to the EventListener.
    RealCall call = new RealCall(client, originalRequest, forWebSocket);
    return call;
  }
RealCall的构造方法：
 private RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
    this.client = client;
    this.originalRequest = originalRequest;
    this.forWebSocket = forWebSocket;
    ////错误重试与重定向拦截器
    this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client, forWebSocket);
  }
同步执行
@Override public Response execute() throws IOException {
////确保线程安全的情况下通过executed来保证每个Call只被执行一次
    synchronized (this) {
      if (executed)
      executed = true;
    }
}
异步请求
@Override public void enqueue(Callback responseCallback) {
//确保线程安全的情况下通过executed来保证每个Call只被执行一次
    synchronized (this) {
      if (executed)
      executed = true;
    }
}
```
在这个方法中可以看到就是把OkhttpClent，Request封装到RealCall中，然后将RealCall返回。

Call只是一个接口，我们创建的实际上是RealCall对象。在RealCall中存在一个 execute 的成员变量，在execute()和enqueue(Callback responseCallback) 方法中都是通过 execute 来确保每个RealCall对象只会被执行一次。

### 4.同步或异步执行请求
同步的代码要少些，而且和异步请求非常相似，这里先分析异步请求后面再过一下同步。

前面看到Call调用的enque方法实际上调用的是RealCall的enque方法
```java
RealCall 的 enque 方法
public void enqueue(Callback responseCallback) {
//synchronized (this) 确保每个call只能被执行一次不能重复执行
    synchronized (this) {
      if (executed) 
      	executed = true;
    }
//client.dispatcher()是前面 OkhttpClient 的 Builder 方法中直接 new 出来的 Dispatcher
    client.dispatcher().enqueue(new AsyncCall(responseCallback));
  }
  
//这个可以看出
OkhttpClient的dispatcher方法
public Dispatcher dispatcher() {
    return dispatcher;
}
```
#### Dispatch
Dispatch也是一个核心的类，这个类必须要看下，可以看到RealCall中的enque方法实际上调用的是Dispatch中的enque方法。
```java
synchronized void enqueue(AsyncCall call) {
//同时请求不能超过并发数(64,可配置调度器调整)
//okhttp会使用共享主机即 地址相同的会共享socket
//同一个host最多允许5条线程通知执行请求
	if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
    	//加入运行队列 并交给线程池执行
	    runningAsyncCalls.add(call);
        //AsyncCall 是一个runnable，放到线程池中去执行，查看其execute实现
	    executorService().execute(call);
	} else {
    	//加入等候队列
	    readyAsyncCalls.add(call);
	}
}
```
上面代码中的属性是什么意思，看一下Diapatch的属性
```java
public final class Dispatcher {
	//最大的请求数量
  private int maxRequests = 64;
  //最大的请求host数
  private int maxRequestsPerHost = 5;
  private Runnable idleCallback;
	//线程池
  private @Nullable ExecutorService executorService;
	//异步准备队列
  private final Deque<AsyncCall> readyAsyncCalls = new ArrayDeque<>();
	//异步运行队列
  private final Deque<AsyncCall> runningAsyncCalls = new ArrayDeque<>();
	//可以知道线程池
  public Dispatcher(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public Dispatcher() {
  }
	//异步执行的线程池
  public synchronized ExecutorService executorService() {
    if (executorService == null) {
    	//核心线程：0 非核心线程：Integer.MAX_VALUE 闲置60秒回收  任务队列
      executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
    }
    return executorService;
  }
```
通过上面的分析可以得出，Dispatch 的 enqueue 方法的流程实际上就是如果同时请求的数量小于64，并且最大请求的主机数小于5条，则将 AsyncCall 添加到运行对列中，并调用线程池进行执行 AsyncCall ,如果不符合这个条件，则将 AsyncCall 添加到等待队列中。

这里用到的线程池可能有些人不太了解，这里详细的说下，这个线程池和四大线程池中的 `CacheThreadPool` 是一样的。

**具体的执行流程：**由于没有核心线程，所以会将任务直接添加到对列中，由于是SynchronousQueue是要给不存储元素的队列，进去后有出去了，然后新建非核心线程来处理任务。

![](https://raw.githubusercontent.com/xioabaiwenwen/upload-images/master/20190320164216.png)

上面的知道了执行流程后，关于Diapatch的分析差不多就结束了，后面还有一点关于这个的队列回收问题，后面再说，接着 AsyncCall 被丢掉了线程池中，AsyncCall 是一个 Runnable ，后面就会执行 AsyncCall 的run 方法了。

##### AsyncCall
分析AsyncCall
AsyncCall 是 RealCall 的内部类
```java
//先不看 AsyncCall 的逻辑，可以看到 extends NamedRunnable
final class AsyncCall extends NamedRunnable {
	//传入的是Callback，作为参数保存了
	private final Callback responseCallback;
    AsyncCall(Callback responseCallback) {
       this.responseCallback = responseCallback;
    }
}
//可以看到 NamedRunnable 是一个抽象类，并实现了Runnable，重写了 run 方法，并再run方法中调用了execute方法。
public abstract class NamedRunnable implements Runnable {
  @Override public final void run() {
      execute();
  }
  protected abstract void execute();
}
```
从上面可以得出，当 AsyncCall 得run方法被调用后，execute方法会被调用。
```java
@Override protected void execute() {
      boolean signalledCallback = false;
      try {
      	//责任链模式
        //拦截器链  执行请求 这步是整个Okhttp源码中 最核心的方法，这里先不看，后面会解释的，可以看到 执行这个方法后，返回了 response
        Response response = getResponseWithInterceptorChain();
        if (retryAndFollowUpInterceptor.isCanceled()) {
          signalledCallback = true;
          // AsyncCall 的构造方法中传递过来的 responseCallback 看上面的
          //回调失败方法
          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
        } else {
          signalledCallback = true;
          //回调成功方法
          responseCallback.onResponse(RealCall.this, response);
        }
      } catch (IOException e) {
        if (signalledCallback) {
          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
        } else {
            eventListener.callFailed(RealCall.this, e);
            responseCallback.onFailure(RealCall.this, e);
        }
      } finally {
      	//移除队列
        client.dispatcher().finished(this);
      }
    }
```
execute中，通过 `getResponseWithInterceptorChain` 获取到了response 后将结果回调给了callback，执行完后的最终都会调用的 client.dispatcher() 前面所说的 Dispatch 中的 finished 方法。

Dispatch#finished方法
```java
void finished(RealCall call) {
    finished(runningSyncCalls, call, false);
  }

 private <T> void finished(Deque<T> calls, T call, boolean promoteCalls) {
    int runningCallsCount;
    Runnable idleCallback;
    synchronized (this) {
    	// 移除队列
      if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
      	//检查是否为异步请求，检查等候的队列 readyAsyncCalls，如果存在等候队列，则将等候队列加入执行队列
      if (promoteCalls) promoteCalls();
      runningCallsCount = runningCallsCount();
      idleCallback = this.idleCallback;
    }

    if (runningCallsCount == 0 && idleCallback != null) {
      idleCallback.run();
    }
  }
```

##### getResponseWithInterceptorChain
这个可以说是最核心的方法了，通过这个方法可以获取 Response。下面进行讲解：

```java
真正的核心方法：
Response getResponseWithInterceptorChain() throws IOException {
    // 责任链
    List<Interceptor> interceptors = new ArrayList<>();
    
    //添加在配置okhttpClient 时设置的intercept 由用户自己设置
    interceptors.addAll(client.interceptors());
    
    //负责处理失败后的重试与重定向
    interceptors.add(retryAndFollowUpInterceptor);
    
    //负责把用户构造的请求转换为发送到服务器的请求 、把服务器返回的响应转换为用户友好的响应 处理 配置请求头等信息
    //从应用程序代码到网络代码的桥梁。首先，它根据用户请求构建网络请求。然后它继续呼叫网络。最后，它根据网络响应构建用户响应。
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    
    //处理 缓存配置 根据条件(存在响应缓存并被设置为不变的或者响应在有效期内)返回缓存响应
    //设置请求头(If-None-Match、If-Modified-Since等) 服务器可能返回304(未修改)
    //可配置用户自己设置的缓存拦截器
    interceptors.add(new CacheInterceptor(client.internalCache()));
    
    //连接服务器 负责和服务器建立连接 这里才是真正的请求网络
    interceptors.add(new ConnectInterceptor(client));
    
    //配置okhttpClient 时设置的 networkInterceptors
    //返回观察单个网络请求和响应的不可变拦截器列表。
    if (!forWebSocket) {
      interceptors.addAll(client.networkInterceptors());
    }
    
    //执行流操作(写出请求体、获得响应数据) 负责向服务器发送请求数据、从服务器读取响应数据
    //进行http请求报文的封装与请求报文的解析
    interceptors.add(new CallServerInterceptor(forWebSocket));

	//创建责任链
    Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
        originalRequest, this, eventListener, client.connectTimeoutMillis(),
        client.readTimeoutMillis(), client.writeTimeoutMillis());

	//执行责任链
    return chain.proceed(originalRequest);
  }
```
这里使用到了责任链的模式，具体的可以参考 责任链模式 

上述代码中可以看出interceptors，是传递到了RealInterceptorChain该类实现Interceptor.Chain，并且执行了chain.proceed(originalRequest)。

具体的处理逻辑在 RealInterceptorChain 的 proceed 方法中，体现了责任链模式。

以前并没有具体的注意具体的责任链调用，RealInterceptorChain 可以说是真正把这些拦截器串起来的一个角色。一个个拦截器就像一颗颗珠子，而 RealInterceptorChain 就是把这些珠子串连起来的那根绳子。下面就是核心代码，仔细理解。
```java

public final class RealInterceptorChain implements Interceptor.Chain {
	//将参数封装
	 public RealInterceptorChain(List<Interceptor> interceptors, StreamAllocation streamAllocation,
      HttpCodec httpCodec, int index, Request request, Call call) {
    	this.interceptors = interceptors;
    	this.connection = connection;
    	this.streamAllocation = streamAllocation;
    	this.httpCodec = httpCodec;
    	this.index = index;
    	this.request = request;
    	this.call = call;
   }
   //处理request，并返回response
	public Response proceed(Request request) throws IOException {
	    return proceed(request, streamAllocation, httpCodec, connection);
	}
    //具体的调用
	public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,
	      RealConnection connection) throws IOException {
		//得到下一次对应的 RealInterceptorChain
	    RealInterceptorChain next = new RealInterceptorChain(interceptors,streamAllocation, httpCodec,connection,
	    	index + 1, //
	        request, call, eventListener,
	        connectTimeout, readTimeout,writeTimeout);
	    //当前次数的 interceptor
	    Interceptor interceptor = interceptors.get(index);
	    //进行拦截处理，并且在 interceptor 链式调用 next 的 proceed 方法
	    Response response = interceptor.intercept(next);
	    return response;
	  }
}
```
上面看到 RealInterceptorChain 实现了 Interceptor.Chain 看一下 Interceptor
```java
public interface Interceptor {
  Response intercept(Chain chain) throws IOException;

  interface Chain {
     Response proceed(Request request) throws IOException;
  }
}
```
大致情形，通过源码大致了解了，逻辑就是，先构建下一个拦截器封装到 RealInterceptorChain ，然后获取当前的拦截器，调用当前的拦截器的 intercept 方法，在这个方法中再次调用下一个拦截器的 intercept 方法，最后将 Response 向上返回。这就是责任链模式。

下面上一张图来提供理解：
![](https://raw.githubusercontent.com/xioabaiwenwen/upload-images/master/%E6%8B%A6%E6%88%AA%E5%99%A8.png)

下面就是各个拦截器的，每个拦截器都是非常难得点，这里先大致得了解下每个拦截器做些什么就行了。

##### RetryAndFollowUpInterceptor
RetryAndFollowUpInterceptor 是用来失败重试以及重定向的拦截器

##### BridgeInterceptor
在 BridgeInterceptor 这一步，先把用户友好的请求进行重新构造，变成了向服务器发送的请求。

之后调用 chain.proceed(requestBuilder.build()) 进行下一个拦截器的处理。

等到后面的拦截器都处理完毕，得到响应。再把 networkResponse 转化成对用户友好的 response

##### CacheInterceptor
CacheInterceptor 做的事情就是根据请求拿到缓存，若没有缓存或者缓存失效，就进入网络请求阶段，否则会返回缓存。

##### ConnectInterceptor
先在连接池中找到可用的连接 resultConnection ，再结合 sink 和 source 创建出 HttpCodec 的对象。

##### CallServerInterceptor
在 CallServerInterceptor 中可见，关于请求和响应部分都是通过 HttpCodec 来实现的。而在 HttpCodec 内部又是通过 sink 和 source 来实现的。所以说到底还是 IO 流在起作用。

对于上面得拦截器，太过于复杂了，本文主要得目的是了解okhttp的大致流程，忽略具体的细节。通过上面的分析基本上可以理解清楚Okhttp的流程，最重要的拦截器没有细说，这是因为里面非常复杂，每一个拦截器都可以分一个小节讲，而且分析整个okhttp的源码不能太沉迷于细节，否则会深入其中无法自拔，导致自己对源码都有恐惧。

具体的拦截器可以参考：
[okhttp源码分析](https://www.jianshu.com/p/37e26f4ea57b)
