package vopen.protocol;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import vopen.response.CourseInfo;
import vopen.response.UiEventTransport;
import vopen.transactions.AddStoreTransaction;
import vopen.transactions.AddStoreTransaction2;
import vopen.transactions.BaseTransaction;
import vopen.transactions.CommentTransaction;
import vopen.transactions.DelStoreTransaction;
import vopen.transactions.DelStoreTransaction2;
import vopen.transactions.FeedBackTransaction;
import vopen.transactions.FeedBackTransactionNew;
import vopen.transactions.GetAboutInfoTransaction;
import vopen.transactions.GetCourseAdTransaction;
import vopen.transactions.GetHeadAdTransation;
import vopen.transactions.GetHomeRecommendInfoTransation;
import vopen.transactions.GetHotWordsTransaction;
import vopen.transactions.GetMobTokenTransation;
import vopen.transactions.GetPushCourseTransaction;
import vopen.transactions.GetRecommAppTransaction;
import vopen.transactions.GetRecommendTransaction;
import vopen.transactions.GetUserBindInfoTrascation;
import vopen.transactions.GetVersionInfoTransaction;
import vopen.transactions.GetVideoDetailTransaction;
import vopen.transactions.GetVideoListTransaction;
import vopen.transactions.LoginTransaction;
import vopen.transactions.PostLogTransaction;
import vopen.transactions.RegisterTransaction;
import vopen.transactions.SendDownloadFeedbackTransaction;
import vopen.transactions.SendSearchFeedbackTransaction;
import vopen.transactions.SendViewCourseFeedbackTransaction;
import vopen.transactions.SyncStoreTransaction;
import vopen.transactions.SyncStoreTransaction2;
import vopen.transactions.SyncTranslateNumTransaction;
import vopen.transactions.UiEventTransportTransaction;
import android.content.Context;

import common.framework.http.HttpEngine;
import common.framework.task.AsyncTransaction;
import common.framework.task.Transaction;
import common.framework.task.TransactionEngine;
import common.framework.task.TransactionListener;

public class VopenService {
	public static final int DEFAULT_RESULTS_COUNT = 30;

	TransactionEngine mTransactionEngine;//事务执行线程

	HttpEngine mAlbumHttpEngine; //协议Http线程

	private GroupTransactionListener mGroupListener;
	public static final String TAG = "VopenService";

	public void addListener(VopenCallBack listener) {
		mGroupListener.addListener(listener);
	}

	public void removeListener(VopenCallBack listener) {
		mGroupListener.removeListener(listener);
	}

	static VopenService mInstance;

	private VopenService() {

		mTransactionEngine = new TransactionEngine();
		mAlbumHttpEngine = new HttpEngine();
		mGroupListener = new GroupTransactionListener();

		HttpDataChannel channel = new HttpDataChannel(mTransactionEngine,
				mAlbumHttpEngine);
		mTransactionEngine.setDataChannel(channel);
	}

	synchronized public static VopenService getInstance() {
		if (mInstance == null) {
			mInstance = new VopenService();
		}
		return mInstance;
	}

	public TransactionEngine getTransationEngine() {

		return mTransactionEngine;
	}

	/**
	 * 注册事务监听器
	 * @param t
	 * @param listener
	 * @return
	 */
	protected int startTransaction(AsyncTransaction t,
			TransactionListener listener) {
		t.setListener(listener);

		return beginTransaction(t);
	}

	/**
	 * 添加一个最简单的事务
	 * @param t
	 * @return
	 */
	public int beginTransaction(Transaction t) {
		mTransactionEngine.beginTransaction(t);
		return t.getId();
	}

	/**
	 * 按id取消事务
	 */
	public void doCancelTransactionByID(int id) {
		mTransactionEngine.cancelTransaction(id);
	}

	/**
	 * 按类型取消事务
	 */
	public void doCancelTransactionByType(int type) {
		Hashtable table = mTransactionEngine.getPaddingTransactionMap();
		Enumeration enumeration = table.elements();
		while (enumeration.hasMoreElements()) {
			Transaction t = (Transaction) enumeration.nextElement();
			if (t instanceof BaseTransaction) {
				if (((BaseTransaction) t).getType() == type) {
					//					((BlogBaseTransaction)t).doCancel();
					mTransactionEngine.cancelTransaction(((BaseTransaction) t)
							.getId());
				}
			}
		}
	}

	/**
	 * 判断是否同类型事务存在
	 * @param type
	 * @return
	 */
	public boolean containTransaction(int type) {
		Hashtable table = mTransactionEngine.getPaddingTransactionMap();
		Enumeration enumeration = table.elements();
		while (enumeration.hasMoreElements()) {
			Transaction t = (Transaction) enumeration.nextElement();
			if (t instanceof BaseTransaction) {
				if (((BaseTransaction) t).getType() == type) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 注销Service方法
	 */
	public void serviceLogout() {

		Hashtable table = mTransactionEngine.getPaddingTransactionMap();
		Enumeration enumeration = table.elements();
		while (enumeration.hasMoreElements()) {
			Transaction t = (Transaction) enumeration.nextElement();
			if (t instanceof BaseTransaction) {
				mTransactionEngine.cancelTransaction(((BaseTransaction) t)
						.getId());
			}
		}
	}

	/**
	 * 关闭Service方法
	 */
	public void shutdown() {
		mInstance = null;

		mTransactionEngine.shutdown();
		mAlbumHttpEngine.shutdown();
	}

	/*以下为协议接口*/
	/**
	 * 获取视频列表
	 * @param 
	 * @return
	 */
	public int doGetVideoList(String cache) {
		GetVideoListTransaction getVListinTransaction = new GetVideoListTransaction(
				mTransactionEngine, cache);
		return startTransaction(getVListinTransaction, mGroupListener);
	}

	/**
	 * <p>获取视频详情,将获取的视频详细信息写入数据表course或更新该表信息,</p>
	 * <p>无网络或获取失败时从本地数据库读取</p>
	 * <p>response成功返回视频详情JSON对象</p>
	 * <p>网络及本地获取都失败时响应失败</p>
	 * @param plid 课程ID
	 * @return
	 */
	public int doGetVideoDetail(String plid) {
		GetVideoDetailTransaction getVideoDetailTransaction = new GetVideoDetailTransaction(
				mTransactionEngine, plid);
		return startTransaction(getVideoDetailTransaction, mGroupListener);
	}

	/**
	 * <p>获取版本信息</p>
	 * <p>首先检查参数setting是否需要显示更新信息</p>
	 * <p>然后与String.xml中版本信息进行对比</p>
	 * <p>需要更新时响应成功，并返回version info</p>
	 * <p>否则响应失败，返回ErrCode不需要更新</p>
	 * @param context
	 * @param version
	 * @return
	 */
	public int doGetVersionInfo(Context context, String version) {
		GetVersionInfoTransaction getVersionInfoTransaction = new GetVersionInfoTransaction(
				mTransactionEngine, context, version);
		return startTransaction(getVersionInfoTransaction, mGroupListener);
	}

	/**
	 * 登录
	 * @param username
	 * @param pwd
	 * @return
	 */
	@Deprecated
	public int doLogin(String username, String pwd) {
		LoginTransaction loginTransaction = new LoginTransaction(
				mTransactionEngine, username, pwd);
		return startTransaction(loginTransaction, mGroupListener);
	}
	
	/**
	 * 注册
	 * @param username
	 * @param pwd
	 * @return
	 */
	public int doRegister(String username, String pwd) {
		RegisterTransaction registerTransaction = new RegisterTransaction(
				mTransactionEngine, username, pwd);
		return startTransaction(registerTransaction, mGroupListener);
	}

	/**
	 * 同步收藏
	 * @param userid
	 * @param cookie
	 * @param isSyncLocal 是否同步本地数据库（当用户刚登陆时同步）
	 * @return
	 */
	@Deprecated
	public int doSyncStore(String userid, String cookie,boolean isSyncLocal) {
		SyncStoreTransaction syncStoreTransaction = new SyncStoreTransaction(
				mTransactionEngine, userid, cookie, isSyncLocal);
		return startTransaction(syncStoreTransaction, mGroupListener);
	}
	
	/**
	 * 同步收藏(新接口)
	 * @param userid
	 * @param mobToken
	 * @param isSyncLocal
	 * @return
	 */
	public int doSyncStore2(String userid, String mobToken,boolean isSyncLocal) {
		SyncStoreTransaction2 syncStoreTransaction = new SyncStoreTransaction2(
				mTransactionEngine, userid, mobToken, isSyncLocal);
		return startTransaction(syncStoreTransaction, mGroupListener);
	}

	/**
	 * 反馈
	 * @param productType 手机型号
	 * @param osVersion 系统版本
	 * @param appVersion 程序版本
	 * @param msg 反馈信息
	 * @param email 反馈email
	 * @return
	 */
	public int doFeedBack(String productType, String osVersion,
			String appVersion, String msg, String email) {
		FeedBackTransaction feedBackTransaction = new FeedBackTransaction(
				mTransactionEngine, productType, osVersion, appVersion, msg,
				email);
		return startTransaction(feedBackTransaction, mGroupListener);
	}

	/**
	 * 获取关于信息
	 * @return
	 */
	public int doGetAboutInfo() {
		GetAboutInfoTransaction getAboutInfoTransaction = new GetAboutInfoTransaction(
				mTransactionEngine);
		return startTransaction(getAboutInfoTransaction, mGroupListener);
	}

	/**
	 * UI事件及数据传递
	 * @param UiEventTransport 包含事件类型及数据
	 * @return
	 */
	public int doNotifyOtherWindow(UiEventTransport event) {
		UiEventTransportTransaction uiEventTransaction = new UiEventTransportTransaction(
				mTransactionEngine, event);
		return startTransaction(uiEventTransaction, mGroupListener);
	}

	/**
	 * 登录用户添加收藏
	 * @param userid 登录用户名
	 * @param playid 课程ID
	 * @param cookie 登录用户cookie
	 * @return
	 */
	@Deprecated
	public int doAddStore(String userid, String playid, String cookie) {
		AddStoreTransaction addStoreTransaction = new AddStoreTransaction(
				mTransactionEngine, userid, playid, cookie);
		return startTransaction(addStoreTransaction, mGroupListener);
	}
	
	/**
	 * 登录用户添加收藏(新接口)
	 * @param userid 登录用户名
	 * @param playid 课程ID
	 * @param token mob token
	 * @return
	 */
	public int doAddStore2(String userid, String playid, String token) {
		AddStoreTransaction2 addStoreTransaction = new AddStoreTransaction2(
				mTransactionEngine, playid, token, userid);
		return startTransaction(addStoreTransaction, mGroupListener);
	}
	

	/**
	 * 登录用户删除收藏
	 * @param userid 登录用户名
	 * @param playid 课程ID
	 * @param cookie 登录用户cookie
	 * @return
	 */
	@Deprecated
	public int doDelStore(Context context, String userid,
			List<String> playidList, String cookie) {
		DelStoreTransaction delStoreTransaction = new DelStoreTransaction(
				mTransactionEngine, context, userid, playidList, cookie);
		return startTransaction(delStoreTransaction, mGroupListener);
	}
	
	/**
	 * 登录用户删除收藏
	 * @param userid 登录用户名
	 * @param playid 课程ID
	 * @param cookie 登录用户cookie
	 * @return
	 */
	public int doDelStore2(Context context, String userid,
			List<String> playidList, String token) {
		DelStoreTransaction2 delStoreTransaction = new DelStoreTransaction2(
				mTransactionEngine, userid, playidList, token);
		return startTransaction(delStoreTransaction, mGroupListener);
	}

	/**
	 * 获取推送课程
	 * @param latestPushId 最近推送课程ID
	 * @return
	 */
	public int doGetPushCourse(Context context, String latestPushId) {
		GetPushCourseTransaction getCoursePushTransaction = new GetPushCourseTransaction(
				mTransactionEngine, context, latestPushId);
		return startTransaction(getCoursePushTransaction, mGroupListener);
	}

	/**
	 * 同步新翻译课数
	 * @param allInfo
	 * @param user
	 * @return
	 */
	public int doSyncTranslateNum(List<CourseInfo> allInfo, String user) {
		SyncTranslateNumTransaction t = new SyncTranslateNumTransaction(
				mTransactionEngine, allInfo, user);
		return startTransaction(t, mGroupListener);
	}

	/**
	 * 下面是评论相关 api
	 */
	public int doGetHotComment(String replyId, int start, int end) {
		CommentTransaction t = CommentTransaction
				.createGetHotCommentTransaction(mTransactionEngine, replyId,
						start, end);
		return startTransaction(t, mGroupListener);
	}

	public int doGetLatestComment(String replyId, int start, int end) {
		CommentTransaction t = CommentTransaction
				.createGetLatestCommentTransaction(mTransactionEngine, replyId,
						start, end);
		return startTransaction(t, mGroupListener);
	}

	public int doGetWholeComment(String replyId, String plid) {
		CommentTransaction t = CommentTransaction.createGetWholeCommentTran(
				mTransactionEngine, replyId, plid);
		return startTransaction(t, mGroupListener);
	}

	public int doPostComment(String threadid, String qoute, String body,
			String userId, String nickName, boolean hideName) {
		CommentTransaction t = CommentTransaction.createPostCommentTran(
				mTransactionEngine, threadid, qoute, body, userId, nickName,
				hideName);
		return startTransaction(t, mGroupListener);
	}

	public int doVoteComment(String docId, String postId) {
		CommentTransaction t = CommentTransaction.createVoteCommentTran(
				mTransactionEngine, docId, postId);
		return startTransaction(t, mGroupListener);
	}

	public int doGetPicShortUrl(String picUrl) {
		CommentTransaction t = CommentTransaction.createGetPicShortUrl(
				mTransactionEngine, picUrl);
		return startTransaction(t, mGroupListener);
	}

	public int doGetCommentShortUrl(String commentUrl) {
		CommentTransaction t = CommentTransaction.createGetCommentShortUrl(
				mTransactionEngine, commentUrl);
		return startTransaction(t, mGroupListener);
	}

	public int doGetCommentCount(String replyId) {
		CommentTransaction t = CommentTransaction.createGetCommentCountTran(
				mTransactionEngine, replyId);
		return startTransaction(t, mGroupListener);
	}

	/**
	 * 反馈日志
	 * @param 
	 * @return
	 */
	public int doPostLog() {
		PostLogTransaction mPostLogTransaction = new PostLogTransaction(
				mTransactionEngine);

		return startTransaction(mPostLogTransaction, mGroupListener);
	}

	/**
	 * 反馈意见
	 * @param 
	 * @return
	 */
	public int doFeedBack(String user, String title, String content,
			String fileId, String contact, Context context) {
		FeedBackTransactionNew mFeedBackTransaction = new FeedBackTransactionNew(
				mTransactionEngine, user, title, content, fileId, contact,
				context);

		return startTransaction(mFeedBackTransaction, mGroupListener);
	}

	/**
	 * 获取推荐App
	 */
	public int doGetRecommApp() {
		GetRecommAppTransaction mGetRecommAppTransaction = new GetRecommAppTransaction(
				mTransactionEngine);
		return startTransaction(mGetRecommAppTransaction, mGroupListener);
	}

	/**
	 * 获取搜索热词
	 */
	public int doGetHotWords() {
		GetHotWordsTransaction transaction = new GetHotWordsTransaction(
				mTransactionEngine);
		return startTransaction(transaction, mGroupListener);
	}

	/**
	 * 获取个性化推荐课程的数量
	 */
	public int doGetRecommends(String uuid, String usrId, int count) {
		GetRecommendTransaction transaction = new GetRecommendTransaction(
				mTransactionEngine, uuid, usrId, count);
		return startTransaction(transaction, mGroupListener);
	}
	
	
	public int doGetHeadAdvertisements(){
		GetHeadAdTransation transaction = new GetHeadAdTransation(mTransactionEngine);
		return startTransaction(transaction, mGroupListener);
	}
	
	/**
	 * 发送用户搜索统计
	 * @param ursId 用户id
	 * @param uuid uuid
	 * @param key 搜索关键字
	 * @param loc 用户的地理位置
	 * @param sys 用户所用的系统
	 * @param deviceId 设备id
	 * @param ip 
	 * @param mac
	 * @param version 系统版本号
	 * @return
	 */
	public int doSendSearchFeedback(String ursId, String uuid, String key,
			String loc, String sys,String deviceId,String ip, String mac,
			String version) {
		SendSearchFeedbackTransaction transaction = new SendSearchFeedbackTransaction(
				mTransactionEngine, ursId, uuid, key, loc, sys, deviceId, ip,
				mac, version);
		return startTransaction(transaction, mGroupListener);
	}
	
	/**
	 * 发送用户观看视频的统计
	 * @param ursId 用户id
	 * @param uuid uuid
	 * @param plid
	 * @param mid
	 * @param loc 用户的地理位置
	 * @param sys 用户所用的系统
	 * @param deviceId 设备id
	 * @param ip
	 * @param mac
	 * @param version 系统版本号
	 * @return
	 */
	public int doSendViewCourseFeedback(String ursId, String uuid, String plid,String mid,
			String loc, String sys,String deviceId,String ip, String mac,
			String version){
		SendViewCourseFeedbackTransaction transaction = new SendViewCourseFeedbackTransaction(
				mTransactionEngine, ursId, uuid, plid, mid, loc, sys, deviceId, ip,
				mac, version);
		return startTransaction(transaction, mGroupListener);
	}
	
	/**
	 * 发送用户下载反馈
	 * @param ursId 用户名
	 * @param uuid 
	 * @param plid 下载的课程id
	 * @param mid 下载的课程列表，用逗号分隔
	 * @param loc 
	 * @param sys
	 * @param deviceId
	 * @param ip
	 * @param mac
	 * @param version
	 * @return
	 */
	public int doSendDownloadFeedback(String ursId, String uuid, String plid,String mid,
			String loc, String sys,String deviceId,String ip, String mac,
			String version){
		SendDownloadFeedbackTransaction transaction = new SendDownloadFeedbackTransaction(
				mTransactionEngine, ursId, uuid, plid, mid, loc, sys, deviceId, ip,
				mac, version);
		return startTransaction(transaction, mGroupListener);
	}

	/**
	 * 获取用于绑定push service的信息
	 */
	public int doGetUserBindInfo (String usrid){
		GetUserBindInfoTrascation transaction = new GetUserBindInfoTrascation(mTransactionEngine, usrid);
		return startTransaction(transaction, mGroupListener);
	}
	
	/**
	 * 获取首页的所有推荐信息
	 */
	public int doGetHomeRecommendInfos(){
		GetHomeRecommendInfoTransation transaction = new GetHomeRecommendInfoTransation(mTransactionEngine);
		return startTransaction(transaction, mGroupListener);
	}
	
	/**
	 * 获取某个视频的广告信息 
	 */
	public int doGetCourseAdInfo(String plid){
		GetCourseAdTransaction transaction = new GetCourseAdTransaction(mTransactionEngine, plid);
		return startTransaction(transaction, mGroupListener);
	}
	
	/**
	 * 获取mob-token。<br>
	 * mob-token用于在用户同步收藏时使用。
	 * @return
	 */
	public int doGetMobToken(String userName){
		GetMobTokenTransation transaction = new GetMobTokenTransation(mTransactionEngine, userName);
		return startTransaction(transaction, mGroupListener);
	}
}
