# View的绘制

## 经典的绘制图：
![](https://upload-images.jianshu.io/upload_images/3985563-5f3c64af676d9aee.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/679)
>在我们的Activity中调用了setContentView之后，会转而执行PhoneWindow的setContentView，它会将DecorView添加到Window中，最终会调用ViewRootImpl的performTraversals；


## measure过程：
![](https://upload-images.jianshu.io/upload_images/3985563-d1a57294428ff668.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/814)

```java
View的方法：
 public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
 		...
        onMeasure(widthMeasureSpec, heightMeasureSpec);
        ...
 }
 具体的ViewGroup的方法：如：AbsoluteLayout
 protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
 	。。。
    measureChildren(widthMeasureSpec, heightMeasureSpec);
    。。。
 }
 ViewGroup提供的方法：
 protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < size; ++i) {
            final View child = children[i];
            if ((child.mViewFlags & VISIBILITY_MASK) != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }
```

measure时View提供的方法，ViewGroup继承View并没有重写这个方法。ViewGroup提供了measureChildren方法用来循环遍历每个子View测量。measure中会调用onMeasure方法，ViewGroup根据自己的特性必须要重写这个方法，View中可以重写也可以不重写。

## layout过程：

![](https://upload-images.jianshu.io/upload_images/3985563-8aefac42b3912539.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1000)

```java
View中的方法：
public void layout(int l, int t, int r, int b) {  

    // 当前视图的四个顶点
    int oldL = mLeft;  
    int oldT = mTop;  
    int oldB = mBottom;  
    int oldR = mRight;  

    // setFrame（） / setOpticalFrame（）：确定View自身的位置
    // 即初始化四个顶点的值，然后判断当前View大小和位置是否发生了变化并返回
    boolean changed = isLayoutModeOptical(mParent) ?
            setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);

    //如果视图的大小和位置发生变化，会调用onLayout（）
    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {  

        // onLayout（）：确定该View所有的子View在父容器的位置     
        onLayout(changed, l, t, r, b);      
  ...
}  
View的onlayout是空实现：
 protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
 }
 
ViewGroup的onLayout是抽象方法：
protected abstract void onLayout(boolean changed,
            int l, int t, int r, int b);

```
测量完View大小后，就需要将View布局在Window中，View的布局主要通过确定上下左右四个点来确定的。

其中布局也是自上而下，不同的是ViewGroup先在layout()中确定自己的布局，然后在onLayout()方法中再调用子View的layout()方法，让子View布局。在Measure过程中，ViewGroup一般是先测量子View的大小，然后再确定自身的大小。

## draw过程：

①绘制背景 background.draw(canvas)
②绘制自己（onDraw）
③绘制Children(dispatchDraw)
④绘制装饰（onDrawScrollBars）

![](https://upload-images.jianshu.io/upload_images/3985563-594f6b3cde8762c7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/939)

```java
public void draw(Canvas canvas) {
// 所有的视图最终都是调用 View 的 draw （）绘制视图（ ViewGroup 没有复写此方法）
// 在自定义View时，不应该复写该方法，而是复写 onDraw(Canvas) 方法进行绘制。
// 如果自定义的视图确实要复写该方法，那么需要先调用 super.draw(canvas)完成系统的绘制，然后再进行自定义的绘制。
     ...
    int saveCount;
    if (!dirtyOpaque) {
          // 步骤1： 绘制本身View背景
        drawBackground(canvas);
    }

        // 如果有必要，就保存图层（还有一个复原图层）
        // 优化技巧：
        // 当不需要绘制 Layer 时，“保存图层“和“复原图层“这两步会跳过
        // 因此在绘制的时候，节省 layer 可以提高绘制效率
        final int viewFlags = mViewFlags;
        if (!verticalEdges && !horizontalEdges) {

        if (!dirtyOpaque) 
             // 步骤2：绘制本身View内容  默认为空实现，  自定义View时需要进行复写
            onDraw(canvas);
    
        ......
        // 步骤3：绘制子View   默认为空实现 单一View中不需要实现，ViewGroup中已经实现该方法
        dispatchDraw(canvas);
  
        ........

        // 步骤4：绘制滑动条和前景色等等
        onDrawScrollBars(canvas);

       ..........
        return;
    }
    ...    
}

View中默认空实现：
protected void onDraw(Canvas canvas) {
 }
```

# View的事件分发：

android事件产生后的传递过程是从Activity--->Window--->View的，而View又分为不包含子 View的View以及包含子View的ViewGroup，事件产生之后首先传递到Activity上面，而Activity接着会传递到 PhoneWindow上，PhoneWindow会传递给RootView，而RootView其实就是DecorView了，接下来便是从 DecorView到View上的分发过程了，具体就可以分成ViewGroup和View的分发两种情况了；


对于ViewGroup而言，当事件分发到当前ViewGroup上面的时候，首先会调用他的dispatchTouchEvent方法，在 dispatchTouchEvent方法里面会调用onInterceptTouchEvent来判断是否要拦截当前事件，如果要拦截的话，就会调用 ViewGroup自己的onTouchEvent方法了，如果onInterceptTouchEvent返回false的话表示不拦截当前事件，那么 事件将会继续往当前ViewGroup的子View上面传递了，如果他的子View是ViewGroup的话，则重复ViewGroup事件分发过程，如 果子View就是View的话，则转到下面的View分发过程；


对于View而言，事件传递过来首先当然也是执行他的dispatchTouchEvent方法了，如果我们为当前View设置了 onTouchListener监听器的话，首先就会执行他的回调方法onTouch了，这个方法的返回值将决定事件是否要继续传递下去了，如果返回 false的话，表示事件没有被消费，还会继续传递下去，如果返回true的话，表示事件已经被消费了，不再需要向下传递了；如果返回false，那么将 会执行当前View的onTouchEvent方法，如果我们为当前View设置了onLongClickListener监听器的话，则首先会执行他的 回调方法onLongClick，和onTouch方法类似，如果该方法返回true表示事件被消费，不会继续向下传递，返回false的话，事件会继续 向下传递，为了分析，我们假定返回false，如果我们设置了onClickListener监听器的话，则会执行他的回调方法onClick，该方法是 没有返回值的，所以也是我们事件分发机制中最后执行的方法了；可以注意到的一点就是只要你的当前View是clickable或者 longclickable的，View的onTouchEvent方法默认都会返回true，也就是说对于事件传递到View上来说，系统默认是由 View来消费事件的，但是ViewGroup就不是这样了；

上面的事件分发过程只是正常情况下的，如果有这样一种情况，比如事件传递到最里层的View之后，调用该View的oonTouchEvent方法返回了 false，那么这时候事件将通过冒泡式的方式向他的父View传递，调用它父View的onTouchEvent方法，如果正好他的父View的 onTouchEvent方法也返回false的话，这个时候事件最终将会传递到Activity的onTouchEvent方法了，也就是最终就只能由 Activity自己来处理了；

事件分发机制需要注意的几点：
 (1)：如果说除Activity之外的View都没有消费掉DOWN事件的话，那么事件将不再会传递到Activity里面的子View了，将直接由Activity自己调用自己的onTouchEvent方法来处理了；
(2)：一旦一个ViewGroup决定拦截事件，那么这个事件序列剩余的部分将不再会由该ViewGroup的子View去处理了，即事件将在此 ViewGroup层停止向下传递，同时随后的事件序列将不再会调用onInterceptTouchEvent方法了；
(3)：如果一个View开始处理事件但是没有消费掉DOWN事件，那么这个事件序列随后的事件将不再由该View来处理，通俗点讲就是你自己没能力就别瞎BB，要不以后的事件就都不给你了；
(4)：View的onTouchEvent方法是否执行是和他的onTouchListener回调方法onTouch的返回值息息相关 的，onTouch返回false，onTouchEvent方法不执行；onTouch返回false，onTouchEvent方法执行，因为 onTouchEvent里面会执行onClick，所以造成了onClick是否执行和onTouch的返回值有了关系

核心代码：
```java
ViewGroup的：
public boolean dispatchTouchEvent(MotionEvent ev) {}
View的：
public boolean dispatchTouchEvent(MotionEvent event) {}
```

# 滑动冲突
在自定义View的过程经常会遇到滑动冲突问题，一般滑动冲突的类型有三种：(1)外部View滑动方向和内部View滑动方向不一致；(2)外部View滑动方向和内部View滑动方向一致；(3)上述两种情况的嵌套；

一般我们解决滑动冲突都是利用的事件分发机制，有两种方式外部拦截法和内部拦截法：

外部拦截法：实 现思路是事件首先是通过父容器的拦截处理，如果父容器不需要该事件的话，则不拦截，将事件传递到子View上面，如果父容器决定拦截的话，则在父容器的 onTouchEvent里面直接处理该事件，这种方法符合事件分发机制；具体实现措施是修改父容器的onInterceptTouchEvent方法， 在达到某一条件的时候，让该方法直接返回true就可以把事件拦截下来进而调用自己的onTouchEvent方法来处理了，但是有一点需要注意的是如果 想要让子View能够收到事件，我们需要在onInterceptTouchEvent方法里面判断如果是DOWN事件的话，返回false，这样后续的MOVE以及UP事件才有机会传递到子View上面，如果你直接在onInterceptTouchEvent方法里面DOWN情况下返回了true，那么后续的MOVE以及UP事件将由当前View的onTouchEvent处理了，这样你的拦截将根本没有意义的，拦截只是在满足一定条件才会拦截，并不是所有情况下都拦截；

内部拦截法：实 现思路是事件从父容器传递到子View上面，父容器不做任何干预性的措施，所有的事件都会传递到子View上面，如果子元素需要改事件，那么就由子元素消 耗掉了，该事件也就不会回传了，如果子元素不需要该事件，那么他就会回传给父容器来处理了；具体实现措施需要借助于 requestDisallowInterceptTouchEvent方法，该方法用来告诉父容器要不要拦截当前事件，为了配合子View能够调用这个 方法成功，父容器必须默认能够拦截除了DOWN事件以外的事件，为什么要除了DOWN事件以外呢？因为如果一旦父容器拦截了DOWN事件，那么后续事件将 不再会传递到子元素了，内部拦截法也就失去作用了；


个人认为外部拦截法是符合正常逻辑的，按照事件隧道式分发过程，如果父容器需要就直接拦截，不需要则传递到子View；内部拦截法相当于人为干预分发这个 过程，我会保证事件先都到子View上面，至于子View需不需要就要看我自己了，如果我不需要就回传给父容器了，需要的话自己就消耗掉了；感觉这两种方 式只是父容器和子View处理事件的优先级不同而已；

# SurfaceView与View的区别
   出现SurfaceView的原因在于：虽然说通常情况下View已经可以满足大部分的绘图需求了，但是在有些时候还是有缺陷的，View是通过刷新来重 绘视图的，Android系统通过发出VSYNC信号来进行屏幕的重绘，刷新的时间间隔是16ms，如果在16ms内刷新完成的话是没有什么影响的，但是 如果刷新的时候执行的操作逻辑太多，那么会出现卡顿的现象，SurfaceView就是解决这个问题的；
   (1)：View主要用于主动更新的情况下，而SurfaceView主要用于被动更新，例如频繁的刷新；
   (2)：View在主线程中对画面进行更新，而SurfaceView通常会通过一个子线程来进行更新；
   (3)：View在绘图的时候是没有使用双缓冲机制的，而SurfaceView在底层实现中使用了双缓冲机制；

