推荐三篇文章：
[Android Service保活方法总结](https://blog.csdn.net/oZhuiMeng123/article/details/82056278)
[2018年Android的保活方案效果统计](https://juejin.im/post/5baedde6f265da0a8d369eb2#heading-3)
[Android 进程保活的一般套路](https://juejin.im/entry/58acf391ac502e007e9a0a11)

### 1.onStartCommand方法，返回START_STICKY
这个是系统自带的，onStartCommand方法必须具有一个整形的返回值，这个整形的返回值用来告诉系统在服务启动完毕后，如果被Kill，系统将如何操作。START_STICKY代表：如果系统在onStartCommand返回后被销毁，系统将会重新创建服务并依次调用onCreate和onStartCommand，这种相当于服务又重新启动恢复到之前的状态了。

### 2.提升Service优先级
在AndroidManifest.xml文件中对于intent-filter可以通过android:priority = “1000”这个属性设置最高优先级，1000是最高值，如果数字越小则优先级越低，同时适用于广播。

### 3.提升Service进程优先级
Android中将进程分成6个等级，由高到低分别是：前台进程、可视进程、次要服务进程、后台进程、内容供应节点以及空进程。当系统进程空间紧张时，会按照优先级自动进行进程回收。可以使用startForeground()将服务设置为前台进程。在onStartCommand中添加如下代码
```java
 Notification.Builder builder=new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("uploadservice");
        builder.setContentText("请保持程序在后台运行");
        builder.setWhen(System.currentTimeMillis());

Intent intent=new Intent(this,MainActivity.class);

PendingIntent pendingIntent=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

builder.setContentIntent(pendingIntent);

NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

Notification notification=builder.build();

startForeground(1,notification);
```
### 4.Service和广播配合
service+broadcast方式：
定义一个广播：
```java
public class BaseReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.my.learn.code.basereceiver")){
            Intent sintent=new Intent("com.my.learn.code.BaseService");
            startService(sintent);
        }
    }
}
```
```java
<receiver android:name="com.my.learn.code.BaseReceiver" >  
        <intent-filter>  
            <action android:name="android.intent.action.BOOT_COMPLETED" />  
            <action android:name="android.intent.action.USER_PRESENT" />  
            <action android:name="com.my.learn.code.basereceiver" />//这个就是自定义的action
        </intent-filter>  
    </receiver>  
```
在onDestory中：
```java
Intent intent = new Intent("com.my.learn.code.basereceiver");

sendBroadcast(intent);  
```
### 5.监听系统广播判断Service状态
通过系统的一些广播，比如：手机重启、界面唤醒、应用状态改变等等监听并捕获到，然后判断我们的Service是否还存活，但要记得加权限。代码如下：
```java
 public class MonitorReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Log.v(TAG,"手机开机");
                Intent sintent=new Intent("com.my.learn.code.BaseService");
                startService(sintent);
            }
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                Log.v(TAG,"解锁");
                Intent sintent=new Intent("com.my.learn.code.BaseService");
                startService(sintent);
            }
        }
    }
    <receiver android:name="com.my.learn.code.MonitorReceiver" >  
    <intent-filter>  
        <action android:name="android.intent.action.BOOT_COMPLETED" />  
        <action android:name="android.intent.action.USER_PRESENT" />  
        <action android:name="android.intent.action.PACKAGE_RESTARTED" />  
        <action android:name="com.my.learn.code.monitor" />
    </intent-filter>  
</receiver> 
```language