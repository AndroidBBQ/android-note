/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: E:\\AndroidProjects\\AidlDemo\\app\\src\\main\\aidl\\gxl\\com\\aidldemo\\IManager.aidl
 */
package gxl.com.aidldemo;

//继承至IInterface(asBinder)
public interface IManager extends android.os.IInterface {
    //Stub 继承 Binder 实现 IManager 是个抽象类
    public static abstract class Stub extends android.os.Binder implements gxl.com.aidldemo.IManager{
		//Binder的唯一标识，一般使用Binder的类名表示，	
        private static final java.lang.String DESCRIPTOR = "gxl.com.aidldemo.IManager";
            
        public Stub(){
            this.attachInterface(this, DESCRIPTOR);
        }
        //用于将服务端的binder对象转换成客户端所需要的aidl接口类型的对象，
		//这种转换时区分线程的，如果客户端和服务端处于同一进程，那么池方法返回的就是服务端Service本身
		//否栈返回的时系统分装后的Stub.proxy对象
        public static gxl.com.aidldemo.IManager asInterface(android.os.IBinder obj){
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof gxl.com.aidldemo.IManager))) {
                return ((gxl.com.aidldemo.IManager)iin);
            }
            return new gxl.com.aidldemo.IManager.Stub.Proxy(obj);
        }
		//返回当前的binder对象
        @Override public android.os.IBinder asBinder(){
            return this;
        }
		//这个方法运行在服务端中的binder线程中，当客户端发起跨进程请求时，远程会通过系统底层封装后交由此方法来处理.
		//服务端通过code可以确定客户端所请求的目标方法是什么，
		//接着从data中取出目标方法所需的参数(如果目标方法有参数的话)，然后执行目标方法。 参考：TRANSACTION_add 
		//当目标方法执行完毕后，就像reply中写入返回值(如果目标方法中有返回值的话)。 参考：TRANSACTION_getStrings
		// 如果onTransact返回为false的话，则会请求失败，可以利用这个特性来做权限验证
        @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException{
            java.lang.String descriptor = DESCRIPTOR;
             switch (code) {
                case INTERFACE_TRANSACTION:
                {
                    reply.writeString(descriptor);
                    return true;
                }
                case TRANSACTION_getStrings:
                {
                    data.enforceInterface(descriptor);
                    java.util.List<java.lang.String> _result = this.getStrings();//执行目标方法
                    reply.writeNoException();
                    reply.writeStringList(_result);//写入返回值
                    return true;
                }
                case TRANSACTION_add:
                {
                    data.enforceInterface(descriptor);
                    java.lang.String _arg0;
                    _arg0 = data.readString();//读取参数
                    this.add(_arg0);//执行方法
                    reply.writeNoException();
                    return true;
                }
                default:
                {
                    return super.onTransact(code, data, reply, flags);
                }
             }
        }

		//代理类 是Stub的内部类
        private static class Proxy implements gxl.com.aidldemo.IManager
        {
            private android.os.IBinder mRemote;
			//要代理的Binder对象
            Proxy(android.os.IBinder remote){
                 mRemote = remote;
            }
			//获取到代理的binder对象
            @Override public android.os.IBinder asBinder()
            {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor()
            {
                 return DESCRIPTOR;
            }
			// 须知：此方法运行在客户端，因为：asInterface方法中，如果是在同一个进程中调用，不会调用到这个方法，
			//    如果不在同一个进程中则会调用这个方法，这个方法被调用，运行在客户端，Stub的transact方法才会在远程调用
			// (1)创建改方法所需要的输入性Parcel对象_data  输出型Parcel对象_reply和返回值对象	 
			// (2)把这个方法的参数写入_data中(如果有参数的话)
			// (3)调用transact方法来发送rpc请求，同时当前线程会被挂起
			// (4)服务端onTransace方法会被调用，制定rpc过程返回后，当前线程继续执行，
			// (5)从_reply中取出rpc过程的返回结果
			// (6)将_reply中的数据返回
            @Override public java.util.List<java.lang.String> getStrings() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain(); //(1)
                android.os.Parcel _reply = android.os.Parcel.obtain();//(1)
                java.util.List<java.lang.String> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getStrings, _data, _reply, 0);//(3)
                    _reply.readException();
                    _result = _reply.createStringArrayList();//(5)
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;//(6)
            }
			//
            @Override public void add(java.lang.String str) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();//(1)
                android.os.Parcel _reply = android.os.Parcel.obtain();//(1)
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(str);//(2)
                    mRemote.transact(Stub.TRANSACTION_add, _data, _reply, 0);//(3)
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
        static final int TRANSACTION_getStrings = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_add = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    }

	//获取Strings方法
    public java.util.List<java.lang.String> getStrings() throws android.os.RemoteException;
	
	//添加String方法
    public void add(java.lang.String str) throws android.os.RemoteException;
}
