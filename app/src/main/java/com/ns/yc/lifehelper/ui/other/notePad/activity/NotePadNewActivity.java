package com.ns.yc.lifehelper.ui.other.notePad.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.lzy.imagepicker.view.CropImageView;
import com.ns.yc.lifehelper.R;
import com.ns.yc.lifehelper.base.BaseActivity;
import com.ns.yc.lifehelper.base.BaseApplication;
import com.ns.yc.lifehelper.cache.CacheNotePad;
import com.ns.yc.lifehelper.ui.me.view.adapter.GlideImageLoader;
import com.ns.yc.lifehelper.ui.other.notePad.bean.NotePadDetail;
import com.ns.yc.lifehelper.utils.AppImageUtils;
import com.ns.yc.lifehelper.utils.AppUtil;
import com.ns.yc.lifehelper.utils.SDCardUtils;
import com.ns.yc.lifehelper.utils.StringUtils;
import com.ns.yc.yccustomtextlib.HyperTextEditor;
import com.pedaily.yc.ycdialoglib.toast.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * ================================================
 * 作    者：杨充
 * 版    本：1.0
 * 创建日期：2017/9/14
 * 描    述：简易记事本
 * 修订历史：
 * ================================================
 */
public class NotePadNewActivity extends BaseActivity implements View.OnClickListener {

    @Bind(R.id.ll_title_menu)
    FrameLayout llTitleMenu;
    @Bind(R.id.toolbar_title)
    TextView toolbarTitle;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.tv_note_detail_date)
    TextView tvNoteDetailDate;
    @Bind(R.id.tv_note_type)
    TextView tvNoteType;
    @Bind(R.id.et_new_title)
    EditText etNewTitle;
    @Bind(R.id.et_new_content)
    HyperTextEditor etNewContent;

    private int maxImgCount = 5;                        //允许选择图片最大数
    private int flag;
    private Realm realm;
    private RealmResults<CacheNotePad> cacheNotePads;
    private NotePadDetail notePadDetail;
    private int id;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if(realm!=null){
            realm.close();
        }*/
    }


    @Override
    protected void onStop() {
        super.onStop();
        //如果APP处于后台，或者手机锁屏，则保存数据
        if(AppUtil.isAppOnBackground(getApplicationContext()) || AppUtil.isLockScreen(getApplicationContext())){
            saveNoteData(true);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backAndExit();
    }

    @Override
    public int getContentView() {
        return R.layout.activity_not_pad_new;
    }


    @Override
    public void initView() {
        initIntentData();
        initToolBar();
        initRealm();
        initImagePicker();
    }


    private void initRealm() {
        if(realm == null){
            realm = BaseApplication.getInstance().getRealmHelper();
        }
    }


    private void initToolBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //去除默认Title显示
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void initIntentData() {
        Intent intent = getIntent();
        if(intent!=null){
            flag = intent.getIntExtra("flag", 0);
            id = intent.getIntExtra("id", 1);
            if(flag==0){
                //新建
                toolbarTitle.setText("新建笔记");
                tvNoteDetailDate.setText(TimeUtils.date2String(new Date()));
                tvNoteType.setText("默认笔记");
            }else {
                //编辑
                toolbarTitle.setText("编辑笔记");
                Bundle bundle = intent.getBundleExtra("data");
                notePadDetail = (NotePadDetail) bundle.getSerializable("notePad");
                showData(notePadDetail);
            }
        }
    }

    @Override
    public void initListener() {
        llTitleMenu.setOnClickListener(this);
        tvNoteType.setOnClickListener(this);
    }

    @Override
    public void initData() {

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_pad_menu_new, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_insert_image:
                insertPhoto();
                break;
            case R.id.action_new_save:
                saveNoteData(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_title_menu:
                backAndExit();
                break;
            case R.id.tv_note_type:
                ToastUtil.showToast(NotePadNewActivity.this,"笔记类型后期添加");
                break;
        }
    }

    /**
     * 初始化图片选择器
     */
    private void initImagePicker() {
        ImagePicker imagePicker = ImagePicker.getInstance();
        imagePicker.setImageLoader(new GlideImageLoader());   //设置图片加载器
        imagePicker.setShowCamera(true);                      //显示拍照按钮
        imagePicker.setCrop(false);                            //允许裁剪（单选才有效）
        imagePicker.setSaveRectangle(true);                   //是否按矩形区域保存
        imagePicker.setSelectLimit(maxImgCount);              //选中数量限制
        imagePicker.setStyle(CropImageView.Style.RECTANGLE);  //裁剪框的形状
        imagePicker.setFocusWidth(800);                       //裁剪框的宽度。单位像素（圆形自动取宽高最小值）
        imagePicker.setFocusHeight(800);                      //裁剪框的高度。单位像素（圆形自动取宽高最小值）
        imagePicker.setOutPutX(1000);                         //保存文件的宽度。单位像素
        imagePicker.setOutPutY(1000);                         //保存文件的高度。单位像素
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            //添加图片返回
            if (data != null && requestCode == 100) {
                //异步方式插入图片
                insertImagesSync(data);
            }
        } else if (resultCode == ImagePicker.RESULT_CODE_BACK) {
            //预览图片返回
            if (data != null && requestCode == 101) {

            }
        }
    }

    /**
     * 展示数据
     */
    private void showData(final NotePadDetail notePadDetail) {
        if(notePadDetail.getCreateTime()!=null){
            tvNoteDetailDate.setText(notePadDetail.getCreateTime());
        }
        tvNoteType.setText(String.valueOf(notePadDetail.getType()));
        etNewTitle.setText(notePadDetail.getTitle());
        Editable eText = etNewTitle.getText();
        Selection.setSelection(eText, eText.length());
        etNewContent.post(new Runnable() {
            @Override
            public void run() {
                //showEditData(myContent);
                etNewContent.clearAllLayout();
                showDataSync(notePadDetail.getContent());
            }
        });
    }

    /**
     * 异步方式显示数据
     */
    private void showDataSync(final String html){
        Subscription subsLoading = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                showEditData(subscriber, html);
            }
        })
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())//生产事件在io
                .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        ToastUtil.showToast(NotePadNewActivity.this,"解析错误：图片不存在或已损坏");
                    }

                    @Override
                    public void onNext(String text) {
                        if (text.contains(SDCardUtils.getPictureDir())) {
                            etNewContent.addImageViewAtIndex(etNewContent.getLastIndex(), text);
                        } else {
                            etNewContent.addEditTextAtIndex(etNewContent.getLastIndex(), text);
                        }
                    }
                });
    }



    /**
     * 显示数据
     */
    protected void showEditData(Subscriber<? super String> subscriber, String html) {
        try{
            List<String> textList = StringUtils.cutStringByImgTag(html);
            for (int i = 0; i < textList.size(); i++) {
                String text = textList.get(i);
                if (text.contains("<img")) {
                    String imagePath = StringUtils.getImgSrc(text);
                    if (new File(imagePath).exists()) {
                        subscriber.onNext(imagePath);
                    } else {
                        ToastUtil.showToast(NotePadNewActivity.this,"图片"+i+"已丢失，请重新插入！");
                    }
                } else {
                    subscriber.onNext(text);
                }
            }
            subscriber.onCompleted();
        }catch (Exception e){
            e.printStackTrace();
            subscriber.onError(e);
        }
    }




    /**
     * 插入图片，打开图片选择器
     */
    private void insertPhoto() {
        ImagePicker.getInstance().setSelectLimit(maxImgCount);
        Intent intent = new Intent(NotePadNewActivity.this, ImageGridActivity.class);
        startActivityForResult(intent, 100);
    }

    /**
     * 保存笔记数据
     * 保存数据,=0销毁当前界面，=1不销毁界面，为了防止在后台时保存笔记并销毁，应该只保存笔记
     */
    private void saveNoteData(boolean isBackground) {
        String title = etNewTitle.getText().toString().trim();
        String type = tvNoteType.getText().toString();
        String time = tvNoteDetailDate.getText().toString();
        String content = getHyperContent();
        if(TextUtils.isEmpty(title)){
            ToastUtil.showToast(NotePadNewActivity.this,"标题不能为空");
            return;
        }
        if(TextUtils.isEmpty(content)){
            ToastUtil.showToast(NotePadNewActivity.this,"内容不能为空");
            return;
        }
        switch (flag){
            case 0:                 //新建笔记保存
                insertNewDataRealm(isBackground,title,type,time,content);
                break;
            case 1:                 //修改笔记保存
                editOldDataRealm(isBackground,title,type,time,content);
                break;
        }
    }

    /**
     * 获取超文本内容
     */
    private String getHyperContent() {
        List<HyperTextEditor.EditData> editList = etNewContent.buildEditData();
        StringBuffer content = new StringBuffer();
        for (HyperTextEditor.EditData itemData : editList) {
            if (itemData.inputStr != null) {
                content.append(itemData.inputStr);
                //Log.d("HyperTextEditor", "commit inputStr=" + itemData.inputStr);
            } else if (itemData.imagePath != null) {
                content.append("<img src=\"").append(itemData.imagePath).append("\"/>");
                //Log.d("HyperTextEditor", "commit imgePath=" + itemData.imagePath);
            }
        }
        return content.toString();
    }


    /**
     * 异步插入图片
     * 用这个一直报错：Center crop requires calling resize with positive width and height
     */
    private ArrayList<String> photos = new ArrayList<>();
    private void insertImagesSync(final Intent data) {
        Subscription subsInsert = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                etNewContent.measure(0, 0);
                int width = ScreenUtils.getScreenWidth();
                int height = ScreenUtils.getScreenHeight();

                ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                photos.clear();
                if (images != null) {
                    for (int a = 0; a < images.size(); a++) {
                        String path = images.get(a).path;
                        photos.add(path);
                    }
                }
                //ArrayList<String> photos = data.getStringArrayListExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                //下面这个是不行的
                //ArrayList<String> photos = data.getStringArrayListExtra(ImagePicker.EXTRA_IMAGE_ITEMS);
                //ArrayList<String> photos = data.getStringArrayListExtra(ImagePicker.EXTRA_FROM_ITEMS);

                //可以同时插入多张图片
                for (String imagePath : photos) {
                    Log.e("NotePadNewActivity", "###path=" + imagePath);
                    Bitmap bitmap = AppImageUtils.getSmallBitmap(imagePath, width, height);     //压缩图片
                    //bitmap = BitmapFactory.decodeFile(imagePath);
                    imagePath = SDCardUtils.saveToSdCard(bitmap);
                    Log.e("NotePadNewActivity", "###imagePath=" + imagePath);
                    subscriber.onNext(imagePath);
                }
                subscriber.onCompleted();
            }
        })
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())                       //生产事件在io
                .observeOn(AndroidSchedulers.mainThread())          //消费事件在UI线程
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        etNewContent.addEditTextAtIndex(etNewContent.getLastIndex(), " ");
                        ToastUtil.showToast(NotePadNewActivity.this, "图片插入成功");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("NotePadNewActivity", e.getMessage() + "===" + e.getLocalizedMessage());
                        ToastUtil.showToast(NotePadNewActivity.this, "图片插入失败" + e.getMessage());
                    }

                    @Override
                    public void onNext(String imagePath) {
                        etNewContent.insertImage(imagePath, etNewContent.getMeasuredWidth());
                    }
                });
    }


    /**
     * 插入一条笔记
     */
    private void insertNewDataRealm(boolean isBackground, String title, String type, String time, String content) {
        initRealm();
        if(realm!=null && realm.where(CacheNotePad.class).findAll()!=null){
            cacheNotePads = realm.where(CacheNotePad.class).findAll();
        }else {
            return;
        }
        realm.beginTransaction();
        CacheNotePad notePad = realm.createObject(CacheNotePad.class);
        notePad.setId(id);
        notePad.setTitle(title);
        notePad.setContent(content);
        notePad.setGroupId(1);
        notePad.setGroupName(type);
        notePad.setType(2);
        notePad.setBgColor("#FFFFFF");
        notePad.setIsEncrypt(0);
        notePad.setCreateTime(TimeUtils.getFriendlyTimeSpanByNow(time));
        realm.insert(notePad);
        realm.commitTransaction();

        flag = 1;//插入以后只能是编辑
        if (!isBackground){
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }
    }


    /**
     * 编辑一条笔记
     */
    private void editOldDataRealm(boolean isBackground, final String title, final String type, final String time, final String content) {
        initRealm();
        RealmResults<CacheNotePad> notePads = realm.where(CacheNotePad.class).equalTo("id", this.id).findAll();
        notePads.size();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                CacheNotePad pad = realm.createObject(CacheNotePad.class);
                pad.setTitle(title);
                pad.setGroupName(type);
                pad.setCreateTime(time);
                pad.setContent(content);
            }
        });
        if (!isBackground){
            finish();
        }
    }


    /**
     * 退出的操作
     */
    private void backAndExit() {
        String title = etNewTitle.getText().toString().trim();
        String type = tvNoteType.getText().toString();
        String time = tvNoteDetailDate.getText().toString();
        String content = getHyperContent();
        if (flag == 0) {                //新建笔记
            if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)){
                saveNoteData(false);
            }
        }else if (flag == 1) {          //编辑笔记
            if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)){
                saveNoteData(false);
            }
        }
        finish();
    }


}
