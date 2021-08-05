package ceui.lisa.activities;

import static ceui.lisa.activities.Shaft.sUserModel;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.URLUtil;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.blankj.utilcode.util.BarUtils;
import com.mxn.soul.flowingdrawer_core.ElasticDrawer;
import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;

import androidx.viewpager.widget.ViewPager;
import ceui.lisa.R;
import ceui.lisa.fragments.BaseFragment;
import ceui.lisa.databinding.FragmentNewSearchBinding;
import ceui.lisa.fragments.FragmentFilter;
import ceui.lisa.fragments.FragmentSearchIllust;
import ceui.lisa.fragments.FragmentSearchNovel;
import ceui.lisa.fragments.FragmentSearchUser;
import ceui.lisa.interfaces.Callback;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;
import ceui.lisa.utils.SearchTypeUtil;
import ceui.lisa.viewmodel.SearchModel;

public class SearchActivity extends BaseActivity<FragmentNewSearchBinding> {

    private final BaseFragment<?>[] allPages = new BaseFragment[]{null, null,null};
    private FragmentFilter fragmentFilter;
    private String keyWord = "";
    private SearchModel searchModel;
    private int index = 0;

    @Override
    protected void initBundle(Bundle bundle) {
        keyWord = bundle.getString(Params.KEY_WORD);
        index = bundle.getInt(Params.INDEX);
        searchModel = new ViewModelProvider(this).get(SearchModel.class);
        searchModel.getKeyword().setValue(keyWord);
        searchModel.getIsNovel().setValue(index == 1);
        searchModel.getNowGo().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                baseBind.drawerlayout.closeMenu(true);
            }
        });
    }

    @Override
    protected int initLayout() {
        return R.layout.fragment_new_search;
    }

    @Override
    protected void initView() {
        final String[] TITLES = new String[]{
                getString(R.string.string_136),
                getString(R.string.string_138),
                getString(R.string.string_432)
        };
        baseBind.searchBox.setText(keyWord);
        baseBind.viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager(), 0) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                if (allPages[position] == null) {
                    if (position == 0) {
                        allPages[position] = FragmentSearchIllust.newInstance();
                    } else if(position == 1){
                        allPages[position] = FragmentSearchNovel.newInstance();
                    } else if(position == 2){
                        allPages[position] = FragmentSearchUser.newInstance(keyWord);
                    }
                }

                return allPages[position];
            }

            @Override
            public int getCount() {
                return TITLES.length;
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return TITLES[position];
            }
        });
        baseBind.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // 通知更改 过滤器-关键字匹配 类型
                if(fragmentFilter != null){
                    if((position == 0 ) && searchModel.getIsNovel().getValue()){
                        searchModel.getIsNovel().setValue(false);
                    }else if(position == 1 && !searchModel.getIsNovel().getValue()){
                        searchModel.getIsNovel().setValue(true);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        baseBind.viewPager.setOffscreenPageLimit(2);
        baseBind.tabLayout.setupWithViewPager(baseBind.viewPager);
        if (index != 0) {
            baseBind.viewPager.setCurrentItem(index);
        }

        if (Shaft.getMMKV().decodeBool(Params.MMKV_KEY_ISSHOWTIPS_SEARCHSORT, true)) {
            tipDialog(mContext);
            baseBind.drawerlayout.openMenu(true);
        }
    }

    @Override
    protected void initData() {
        baseBind.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.finish();
            }
        });
        baseBind.toolbar.inflateMenu(R.menu.illust_filter);
        baseBind.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_filter) {
                    Common.hideKeyboard(mActivity);
                    if (baseBind.drawerlayout.isMenuVisible()) {
                        baseBind.drawerlayout.closeMenu(true);
                    } else {
                        baseBind.drawerlayout.openMenu(true);
                    }
                    return true;
                }
                return false;
            }
        });
        baseBind.searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchModel.getKeyword().setValue(baseBind.searchBox.getText().toString());
            }
        });
        baseBind.searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (TextUtils.isEmpty(baseBind.searchBox.getText().toString()) && TextUtils.isEmpty(searchModel.getStarSize().getValue())) {
                    Common.showToast(getString(R.string.string_139));
                    return false;
                }

                String keyword = baseBind.searchBox.getText().toString();

                if (URLUtil.isValidUrl(keyword)) {
                    try {
                        Intent intent = new Intent(mContext, OutWakeActivity.class);
                        intent.setData(Uri.parse(keyword));
                        startActivity(intent);
                        mActivity.finish();
                    } catch (Exception e) {
                        Common.showToast(e.toString());
                        e.printStackTrace();
                    }
                }
                else if(Common.isNumeric(keyword)){
                    QMUITipDialog tipDialog = new QMUITipDialog.Builder(mContext)
                            .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                            .setTipWord(getString(R.string.string_429))
                            .create();
                    tipDialog.show();
                    //先假定为作品id
                    PixivOperate.getIllustByID(sUserModel, Integer.valueOf(keyword), mContext, new Callback<Void>() {
                        @Override
                        public void doSomething(Void t) {
                            PixivOperate.insertSearchHistory(keyword, SearchTypeUtil.SEARCH_TYPE_DB_ILLUSTSID);
                            tipDialog.dismiss();
                            mActivity.finish();
                        }
                    }, new Callback<Void>() {
                        @Override
                        public void doSomething(Void t) {
                            tipDialog.dismiss();
                            Intent intent = new Intent(mContext, UserActivity.class);
                            intent.putExtra(Params.USER_ID, Integer.valueOf(keyword));
                            startActivity(intent);
                            mActivity.finish();
                        }
                    });
                }
                else{
                    searchModel.getNowGo().setValue("search_now");
                    Common.hideKeyboard(mActivity);
                }

                return true;
            }
        });

        baseBind.drawerlayout.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL);
        fragmentFilter = new FragmentFilter();
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (!fragmentFilter.isAdded()) {
            fragmentManager.beginTransaction()
                    .add(R.id.id_container_menu, fragmentFilter)
                    .commitNowAllowingStateLoss();
        } else {
            fragmentManager.beginTransaction()
                    .show(fragmentFilter)
                    .commitNowAllowingStateLoss();
        }
    }

    private void tipDialog(Context context){
        QMUIDialog qmuiDialog = new QMUIDialog.MessageDialogBuilder(context)
                .setTitle(context.getString(R.string.string_433))
                .setMessage(context.getString(R.string.string_434))
                .setSkinManager(QMUISkinManager.defaultInstance(context))
                .addAction(context.getString(R.string.string_190), new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        Shaft.getMMKV().encode(Params.MMKV_KEY_ISSHOWTIPS_SEARCHSORT, false);
                        dialog.dismiss();
                    }
                })
                .create();
        qmuiDialog.show();
    }
}
