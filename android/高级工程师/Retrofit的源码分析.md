# 一些介绍
具体的读下面两篇文章：
[观战Retrofit开发中的哪点事](https://www.jianshu.com/p/07794cb4972a)
[Retrofit实现持久化Cookie的三种方案](https://www.jianshu.com/p/fcccf5907bab)

关于Retrofit的使用大致就是上面的了。

依赖：
```java
// retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.3.0'
//gson转换的
    implementation 'com.squareup.retrofit2:converter-gson:2.3.0'
//配合Rxjava使用的
    implementation 'com.squareup.retrofit2:adapter-rxjava2:2.3.0'
```

下面必须要记住的：
## 请求方法注解
该类型的注解用于标注不同的http请求方式，主要有以下几种

| 注解     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| @GET     | 表明这是get请求                                              |
| @POST    | 表明这是post请求                                             |
| @HEAD    | 表明这是一个head请求                                         |
| @HTTP    | 通用注解,可以替换以上所有的注解，其拥有三个属性：method，path，hasBody |
| @PUT     | 表明这是put请求                                              |
| @DELETE  | 表明这是delete请求                                           |
| @PATCH   | 表明这是一个patch请求，该请求是对put请求的补充，用于更新局部资源 |
| @OPTIONS | 表明这是一个option请求                                       |

post表示新增，put可以理解为完整替换，而patch则是更新资源。

## 请求头注解
该类型的注解用于为请求添加请求头。

| 注解     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| @Headers | 用于添加固定请求头，可以同时添加多个。通过该注解添加的请求头不会相互覆盖，而是共同存在 |
| @Header  | 作为方法的参数传入，用于添加不固定值的Header，该注解会更新已有的请求头 |

### 请求和响应格式注解
该类型的注解用来标注请求参数的格式，有些需要结合上面请求和响应格式的注解一起使用。

| 名称            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| @FormUrlEncoded | 表示请求发送编码表单数据，每个键值对需要使用@Field注解       |
| @Multipart      | 表示请求发送multipart数据，需要配合使用@Part                 |
| @Streaming      | 表示响应用字节流的形式返回.如果没使用该注解,默认会把数据全部载入到内存中.该注解在在下载大文件的特别有用 |

### 请求参数类注解
该类型的注解用来标注请求参数的格式，有些需要结合上面请求和响应格式的注解一起使用。

| 名称      | 说明                                                         |
| --------- | ------------------------------------------------------------ |
| @Body     | 多用于post请求发送非表单数据,比如想要以post方式传递json格式数据 |
| @Filed    | 多用于post请求中表单字段,Filed和FieldMap需要FormUrlEncoded结合使用 |
| @FiledMap | 和@Filed作用一致，用于不确定表单参数                         |
| @Part     | 用于表单字段,Part和PartMap与Multipart注解结合使用,适合文件上传的情况 |
| @PartMap  | 用于表单字段,默认接受的类型是Map<String,REquestBody>，可用于实现多文件上传 |
| @Path     | 用于url中的占位符                                            |
| @Query    | 用于Get中指定参数                                            |
| @QueryMap | 和Query使用类似                                              |
| @Url      | 指定请求路径                                                 |

## 一些使用
常见的几种形式
```java
@GET("article/list")
    Call<ResponseBody> getList(@Query("page") int page, @Query("size") int size);

    @POST("login")
    @FormUrlEncoded
    Call<ResponseBody> login(@Field("name")String name,@Field("password")String password);

    // { aaa:aa } 这种形式
    @POST()
    Call<ResponseBody> getArti(@Body RequestBody body);
    //使用
    Gson gson = new Gson();
    JsonObject object = new JsonObject();
        object.addProperty("id",001);
    String json = gson.toJson(object);
    RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json);

    // 上传 {JsonData:{}} 的同时一张图片
    @POST("Transaction/Receive")
    @Multipart
    Call<ResponseBody> receiveTransaction(@Part("JsonData") RequestBody requestbody, @Part MultipartBody.Part request_img_part);

    // 上传 {JsonData {}} 的同时上传多张图片
    @POST("OptBrokenDocument/Create")
    @Multipart
    Call<ResponseBody> postCreateDocument(@Part("JsonData") RequestBody requestbody, @Part List<MultipartBody.Part> request_img_part);
    //使用
    Gson gson = new Gson();
    String jsonStr = gson.toJson(obj);
    RequestBody requestbody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonStr);



    @POST("OptLineCross/Update")
    @Multipart
    Call<BaseCallBack<String>> postCrossUpdate(@PartMap Map<String, RequestBody> params, @Part List<MultipartBody.Part> request_img_part);
    //参数
    Map<String, RequestBody> params = new HashMap<>();
    RequestBody contact = RequestBody.create(MediaType.parse("multipart/form-data"), 23131321);
    params.put("BrokenDocumentId", contact);
    //图片列表
    List<MultipartBody.Part> requestImgParts = new ArrayList<>();
    for (int i = 0; i < datas.size(); i++) {
        File file = new File((String) datas.get(i).get("path"));
        RequestBody imgFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part requestImgPart = MultipartBody.Part.createFormData("PatrolImage" + i, file.getName(), imgFile);
        requestImgParts.add(requestImgPart);
    }
```
### 上传单张图片
1.api
```java
@Multipart
@POST("mobile/upload")
Call<ResponseBody> upload(@Part MultipartBody.Part file);
```
2.构建MultipartBody对象
```java
File file = new File(url);
//构建requestbody 
RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file); 
//将resquestbody封装为MultipartBody.Part对象 
MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
```
###上传多张图片
1.api
```java
@Multipart
@POST("upload/upload")
Call<ResponseBody> upload(@PartMap Map<String, MultipartBody.Part> map);
```
2.构建Map，和上面的差不多。

## 下载
1.api
```
@GET
Call<ResponseBody> downloadPicture(@Url String fileUrl);
```
2.获取ResponseBody对象
```java
 InputStream is = responseBody.byteStream(); 
String[] urlArr = url.split("/"); 
File filesDir = Environment.getExternalStorageDirectory(); 
File file = new File(filesDir, urlArr[urlArr.length - 1]); 
if (file.exists()) file.delete();

```
如果文件比较大的话可能会发生IO异常，解决方式就是在api上添加@Streaming注解
```java
@Streaming
@GET
Observable<ResponseBody> downloadPicture(@Url String fileUrl);
```

# 使用
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.github.com/")
    .build();

GitHubService service = retrofit.create(GitHubService.class);
Call<List<Repo>> repos = service.listRepos();
repos.enqueue(new Callback<List<Repo>>(){
    @Override
    public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response){

    }

    @Override
    public void onFailure(Call<List<Repo>> call, Throwable t){

    }
});
```

# 源码解析
## 第一步解析Builder.builder

先看第一步`new Retrofit.Builder()`做了什么
```java
public Builder() {
      this(Platform.get());
}

private static final Platform PLATFORM = findPlatform();
static Platform get() {
  return PLATFORM;
}

private static Platform findPlatform() {
    try {
      Class.forName("android.os.Build");
      if (Build.VERSION.SDK_INT != 0) {
        return new Android();//如果是Android平台
      }
    }
    try {
      Class.forName("java.util.Optional");
      return new Java8();//如果是java平台
    }
    return new Platform();
  }

static class Android extends Platform {
	//返回的是主线程Executor
    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }
 	//默认的CallAdapter   将Executor封装成 CallAdapterFactory 并返回
    @Override CallAdapter.Factory defaultCallAdapterFactory(Executor
    callbackExecutor) {
    	//将Exector封装，并返回
      return new ExecutorCallAdapterFactory(callbackExecutor);
    }
	//主线程Handler  可以看到runnable被发送到了主线程中运行了
    static class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());
		//这个与最后的enque返回的结果在主线程有关
      @Override public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
```
可以看到获取平台信息，并作为参数保存在了Builder中


接着`baseUrl`的实现：
```java
这里可以看到先将String类型的baseUrl封装成HttpUrl，然后将httpUrl当做当前的参数
public Builder baseUrl(String baseUrl) {
      return baseUrl(HttpUrl.get(baseUrl));
    }

public Builder baseUrl(HttpUrl baseUrl) {
      this.baseUrl = baseUrl;
      return this;
}

//这是我们经常使用的
List<Converter.Factory> converterFactories = new ArrayList<>();
List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
//添加conver方法 比如gson的转换
 public Builder addConverterFactory(Converter.Factory factory) {
   converterFactories.add(checkNotNull(factory, "factory == null"));
   return this;
 }
//可以看到conber和adapter都被直接添加到集合中了 这个adapter比如rxjava的转换
public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
  callAdapterFactories.add(checkNotNull(factory, "factory == null"));
  return this;
}

//设置客户端，可以自定义好okhttpclient后通过这个方法传递过来
public Builder client(OkHttpClient client) {
  return callFactory(checkNotNull(client, "client == null"));
}
//也就是作为一个参数
public Builder callFactory(okhttp3.Call.Factory factory) {
  this.callFactory = checkNotNull(factory, "factory == null");
  return this;
}
```

最后再看`build`方法
```java
public Retrofit build() {
		//请求必须要穿的有baseUrl
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }
		//如果第二步中没有传递OkhttpClient,使用的是默认的
      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }
      
		//设置  Executor
      Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
      	//看第一步：这个platform其实是Android 返回的是MainThreadExecutor
        callbackExecutor = platform.defaultCallbackExecutor();
      }

      //将CallAdapterFactories添加到集合
      List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
    //看第一步中的：platform其实是Android，返回的是 ExecutorCallAdapterFactory
    callAdapterFactories.addAll(platform.defaultCallAdapterFactories(callbackExecutor));

		//其中并没有添加数据
      List<Converter.Factory> converterFactories = new ArrayList<>(this.converterFactories);
      
	//将参数封装到Retrofit中
      return new Retrofit(callFactory, baseUrl, unmodifiableList(converterFactories),
          unmodifiableList(callAdapterFactories), callbackExecutor, validateEagerly);
    }
```

## 第二步解析 create
把定义的接口转换成接口实例
```java
 public <T> T create(final Class<T> service) {
   	//可以看到动态代理模式
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();
          private final Object[] emptyArgs = new Object[0];
          //当我们调用代理类里面的方法时invoke()都会被执行
          public Object invoke(Object proxy, Method method, @Nullable Object[] args){
			。。。最重要的三步
            //1.将method封装成ServiceMethod
            ServiceMethod<Object, Object> serviceMethod =
                (ServiceMethod<Object, Object>) loadServiceMethod(method);
            //2.通过serviceMethod, args获取到okHttpCall 对象
            OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
            //3.再把okHttpCall进一步封装并返回Call对象
            return serviceMethod.callAdapter.adapt(okHttpCall);
        });
  }
```
上面的三步分别来看：

**第一步：将method封装成ServiceMethod**
```java
ServiceMethod<?, ?> loadServiceMethod(Method method) {
	//缓存处理暂时不看
    result = new ServiceMethod.Builder<>(this, method).build();
    return result;
}

//ServiceMethod的Builder构造方法
Builder(Retrofit retrofit, Method method) {
      this.retrofit = retrofit;
      this.method = method;
      this.methodAnnotations = method.getAnnotations();
      this.parameterTypes = method.getGenericParameterTypes();
      this.parameterAnnotationsArray = method.getParameterAnnotations();
    }

//ServiceMethod的Builder的build方法
public ServiceMethod build() {
		//其实是ExecutorCallAdapterFactory，参考下面分析
      callAdapter = createCallAdapter();//第一步
      responseType = callAdapter.responseType();
      responseConverter = createResponseConverter();//第二步
      return new ServiceMethod<>(this);//第三步
}
```
上面的方法很好理解，先将retrofit和method封装到了Builder中，然后通过build构造了一个SErviceMethod。

ServiceMethod的build方法中最重要的就是其中的三个重要方法：

(1):createCallAdapter
```java
private CallAdapter<T, R> createCallAdapter() {
	  //获取方法的返回类型
      Type returnType = method.getGenericReturnType();
     //获取方法的注解
      Annotation[] annotations = method.getAnnotations();
      //调用retrofit的callAdapter方法
      return (CallAdapter<T, R>) retrofit.callAdapter(returnType, annotations);
}
```
具体的获取方法的返回类型，和方法的注解我们现在不关心，最重要的看 **retrofit** 的callAdapter方法。
```java
public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
  return nextCallAdapter(null, returnType, annotations);
}
//具体的调用方法实现
public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType,
      Annotation[] annotations) {
  	//省略了一些不重要的东西
	//这个skipPast上面可以看到传过来的是null 所有start等于0
    int start = adapterFactories.indexOf(skipPast) + 1;
    //从start开始遍历adapterFactories
    for (int i = start, count = adapterFactories.size(); i < count; i++) {
    	//返回的是adapterFactories集合中的start个的get方法调用后的CallAdapter
      CallAdapter<?, ?> adapter = adapterFactories.get(i).get(returnType, annotations, this);
        return adapter;
    }
  }
```
Retrofit中的callAdapter方法调用了nextCallAdapter方法，在这个方法中从start开始遍历了adapterFactories集合，这个集合是干什么用的，这需要看前面的Retrofit的Builder的build方法。
```java
final List<CallAdapter.Factory> adapterFactories;
Builder{
	public Retrofit build() {
    	//这个集合最后会传递个Retrofit，并赋值给adapterFactories
    	List<CallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
        //这个通过第一步中可以得到 这个添加的是 ExecutorCallAdapterFactory
      adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));
    }
}
```
通过上面的分析可以得出结果createCallAdapter获取到的CallAdapter是ExecutorCallAdapterFactory。

（2）createResponseConverter
先来看下源码
```java
private Converter<ResponseBody, T> createResponseConverter() {
      Annotation[] annotations = method.getAnnotations();
     return retrofit.responseBodyConverter(responseType, annotations);
```
从这第一步就可以看出来和上面的逻辑是一样的。

(3)新建ServiceMethod
```java
ServiceMethod(Builder<R, T> builder) {
    this.callFactory = builder.retrofit.callFactory();
    this.callAdapter = builder.callAdapter;//ExecutorCallAdapterFactory
    this.baseUrl = builder.retrofit.baseUrl();//baseurl
    this.responseConverter = builder.responseConverter;
    this.httpMethod = builder.httpMethod;
	。。。
  }
```

**第二步：通过serviceMethod, args获取到okHttpCall 对象**
```java
  OkHttpCall(ServiceMethod<T, ?> serviceMethod, @Nullable Object[] args) {
    this.serviceMethod = serviceMethod;
    this.args = args;
  }
```

可以看到这步非常简单，就是将方法和参数封装。

**第三步：把okHttpCall进一步封装并返回Call对象**

先再来看下再Retrofit得create方法中对这步的调用
```java
serviceMethod.callAdapter.adapt(okHttpCall)
```
关于 serviceMethod.callAdapter 是什么，从第一步中可以看到是ExecutorCallAdapterFactory。所以要找到 ExecutorCallAdapterFactory 的 adapter 中的方法调用，可是到ExecutorCallAdapterFactory 中确找不到 adapter ，只有其中的 get 方法，对 adapter 进行了间接调用。
```java
public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    final Type responseType = Utils.getCallResponseType(returnType);
    //重点是这个方法 CallAdapter 匿名内部类
    return new CallAdapter<Object, Call<?>>() {
        public Type responseType() {
            return responseType;
        }
        public Call<Object> adapt(Call<Object> call) {
        	//这里ExecutorCallAdapterFactory.this.callbackExecutor其实是MainThreadExecutor 具体下面会分析。
            return new ExecutorCallAdapterFactory.ExecutorCallbackCall(ExecutorCallAdapterFactory.this.callbackExecutor, call);
            }
      };
}
```
可以看到adapter中，实际调用了 `get` 方法中的匿名内部类中的 `adapter` 方法，其中传递的两个参数，一个是 `ExecutorCallAdapterFactory.this.callbackExecutor`，另一个是 call，call 很好理解就是传入的OkhttpCall,那么 callbackExecutor 呢？ 这就要回去看第一步了。
```java
Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
        callbackExecutor = platform.defaultCallbackExecutor();
      }
//可以看到callbackExecutor代表的是platform.defaultCallbackExecutor()
//platform代表的是Android平台，所以他的defaultCallbackExecutor其实是MainThreadExecutor
      List<CallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
      adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

-----
static class Android extends Platform {
    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }
    static class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
```
从上面的分析中可以得到callbackExecutor其实就是MainThreadExecutor，call是OkhttpCall，下面来看ExecutorCallAdapterFactory的ExecutorCallbackCall具体做了什么？
```java
 static final class ExecutorCallbackCall<T> implements Call<T> {
        final Executor callbackExecutor;
        final Call<T> delegate;
		//将callbackExecutor,delegate作为参数
        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
            this.callbackExecutor = callbackExecutor;//MainThreadExecutor
            this.delegate = delegate;//OkhttpCall
        }
		//异步
        public void enqueue(final Callback<T> callback) {
            Utils.checkNotNull(callback, "callback == null");
            this.delegate.enqueue(new Callback<T>() {
                public void onResponse(Call<T> call, final Response<T> response) {
                	//调用了MainThreadExecutor
                    ExecutorCallbackCall.this.callbackExecutor.execute(new Runnable() {
                        public void run() {
                        	//OkhttpCall
                            if (ExecutorCallbackCall.this.delegate.isCanceled()) {
                                callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
                            } else {
                                callback.onResponse(ExecutorCallbackCall.this, response);
                            }

                        }
                    });
                }

                public void onFailure(Call<T> call, final Throwable t) {
                    ExecutorCallbackCall.this.callbackExecutor.execute(new Runnable() {
                        public void run() {
                            callback.onFailure(ExecutorCallbackCall.this, t);
                        }
                    });
                }
            });
        }
		同步
        public Response<T> execute() throws IOException {
            return this.delegate.execute();
        }
		//取消
        public void cancel() {
            this.delegate.cancel();
        }
		//是否取消
        public boolean isCanceled() {
            return this.delegate.isCanceled();
        }
    }
```
它的做法非常简单，就是把他们当作参数保存起来，这个类非常重要，上面之所以给出全部的方法，从里面的方法也可以看出这些逻辑的重要性，在这里先有个印象，后面还会提到这里。这里要记得，传入的两个参数一个是MainThreadExecutor，一个是OkhttpCall。

这一步还有一个重要的要讲，就是这里返回的Call<>其实就是上面的ExecutorCallAdapterFactory.ExecutorCallbackCall。
```java
  return new ExecutorCallAdapterFactory.ExecutorCallbackCall(ExecutorCallAdapterFactory.this.callbackExecutor, call);
```
以上就是 `createa` 方法的逻辑。

# 第三步：同步或异步请求数据
在第二步获取到了Call之后，可以调用execute来执行同步请求，调用enqueue来执行异步请求。

从第二步中可以知道获取到的Call其实是ExecutorCallbackCall，所以执行call的调用execute来执行同步请求，调用enqueue来执行异步请求,其实调用的就是ExecutorCallbackCall中的execute和enqueue方法。

其实第二步的最后也看到了这个代码，由于这比较重要，再来看一遍。
```java
 static final class ExecutorCallbackCall<T> implements Call<T> {
        final Executor callbackExecutor;
        final Call<T> delegate;
		//传入的是MainThreadExecutor，OkhttpCall 具体的看第二步
        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
            this.callbackExecutor = callbackExecutor;//MainThreadExecutor
            this.delegate = delegate;//OkhttpCall
        }
		//这里处理异步请求，看一下具体逻辑
        public void enqueue(final Callback<T> callback) {
            //实际调用的是OkhttpCall的enque方法
            this.delegate.enqueue(new Callback<T>() {
            	//异步请求成功
                public void onResponse(Call<T> call, final Response<T> response) {
                	//调用了MainThreadExecutor的execute
                    ExecutorCallbackCall.this.callbackExecutor//MainThreadExecutor
                    .execute(
                    	new Runnable() {
                        	public void run() {
                        		////切换到了主线程中了
                        	    if (ExecutorCallbackCall.this.delegate//OkhttpCall
                                .isCanceled()) {//如果取消了
                                	//执行OkhttpCall的失败方法
                        	        callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
                        	    } else {
                                	//OkhttpCall的onResponse方法
                        	        callback.onResponse(ExecutorCallbackCall.this, response);
                        	    }
	
                        	}
                    });
                }

                public void onFailure(Call<T> call, final Throwable t) {
                	//异步请求失败
                    //调用了MainThreadExecutor的execute方法，
                    ExecutorCallbackCall.this.callbackExecutor//MainThreadExecutor
                    .execute(new Runnable() {
                        public void run() {
                        	//切换到了主线程中了
                            callback.onFailure(ExecutorCallbackCall.this, t);
                        }
                    });
                }
            });
        }
		同步
        public Response<T> execute() throws IOException {
        	//实际调用的是Okhttp的execute的方法
            return this.delegate.execute();
        }
		//取消
        public void cancel() {
            this.delegate.cancel();
        }
		//是否取消
        public boolean isCanceled() {
            return this.delegate.isCanceled();
        }
    }
```
上面对代码中进行了详细的分析，你会发现retrofit的源码时如此的美妙，如果是行的是同步方法则直接调用了原来的OkhttpCall的execute方法，如果执行的是异步方法，则通过了MainThreadExecutor来将线程切换到主线程然后再处理结果。

OkhttpCall的execute方法
```java
//同步
@Override public Response<T> execute() throws IOException {
}
//异步
@Override public void enqueue(final Callback<T> callback) {
}
```
具体的怎样处理就是Okhttp的源码分析了。

以上就是Retrofit的源码分析了，本文只是关注了源码中最重要的东西，忽略了一些具体的内容，大致的过程就向上面的这样，整个读过来真的是佩服这个框架设计者的思想，框架真的是太完美了。