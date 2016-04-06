package com.ghkjgod.lightnovel.reader.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.ghkjgod.lightnovel.MyApp;
import com.ghkjgod.lightnovel.global.GlobalConfig;
import com.ghkjgod.lightnovel.global.api.Wenku8Error;
import com.ghkjgod.lightnovel.lightnovel.R;
import com.ghkjgod.lightnovel.lightnovel.ViewImageDetailActivity;
import com.ghkjgod.lightnovel.reader.loader.WenkuReaderLoader;
import com.ghkjgod.lightnovel.reader.setting.WenkuReaderSettingV1;
import com.ghkjgod.lightnovel.util.LightTool;
import com.ghkjgod.lightnovel.util.SingletonThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * Created by MewX on 2015/7/8.
 * <p>
 * Implement whole view of page, and use full screen page size.
 * <p>
 * Default Elements:
 * - Top: ChapterTitle, WIFI/DC
 * - Bot: Battery, Paragraph/All, CurrentTime
 * <p>
 * Click Elements:
 * - Top: NovelTitle
 * - Bot: ToolBar
 */
public class WenkuReaderPageView extends View {
    // enum
    public enum LOADING_DIRECTION {
        FORWARDS, // go to next page
        CURRENT, // get this page
        BACKWARDS // go to previous page
    }

    // class
    private class LineInfo {
        WenkuReaderLoader.ElementType type;
        String text;
    }

    List<LineInfo> lineInfoList;

    private class BitmapInfo {
        int idxLineInfo;
        int width, height;
        int x_beg, y_beg;
        Bitmap bm;
    }

    List<BitmapInfo> bitmapInfoList;

    // core variables
    static private boolean inDayMode = true;
    static private String sampleText = "轻";
    static private WenkuReaderLoader mLoader;
    static private WenkuReaderSettingV1 mSetting;
    static private int pxLineDistance, pxParagraphDistance, pxParagraphEdgeDistance, pxPageEdgeDistance, pxWidgetHeight;
    static private Point screenSize;
    private Point textAreaSize;
    static private Typeface typeface;
    static private TextPaint textPaint, widgetTextPaint;
    static private int fontHeight, widgetFontHeihgt;
    private int lineCount;

    // background
    static private Bitmap bmBackgroundYellow, bmTextureYellow[];
    static private BitmapDrawable bmdBackground;
    static private Random random = new Random();
    static private boolean isBackgroundSet = false;

    // vars
    private int firstLineIndex;
    private int firstWordIndex;
    private int lastLineIndex;
    private int lastWordIndex; // last paragraph's last word's index

    // view components (battery, page number, etc.)


    // useless constructs
//    public WenkuReaderPageView(Context context) {
//        super(context);
//        Log.e("MewX", "-- view: construct 1");
//    }
//
//    public WenkuReaderPageView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        Log.e("MewX", "-- view: construct 2");
//    }
//
//    public WenkuReaderPageView(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        Log.e("MewX", "-- view: construct 3");
//    }

    static public boolean getInDayMode() {
        return inDayMode;
    }

    static public boolean switchDayMode() {
        inDayMode = !inDayMode;
        return inDayMode;
    }

    /**
     * Set view static variables, before first onDraw()
     *
     * @param wrl loader
     * @param wrs setting
     */
    static public void setViewComponents(WenkuReaderLoader wrl, WenkuReaderSettingV1 wrs, boolean forceMode) {
        mLoader = wrl;
        mSetting = wrs;
        pxLineDistance = LightTool.dip2px(MyApp.getContext(), mSetting.getLineDistance());
        pxParagraphDistance = LightTool.dip2px(MyApp.getContext(), mSetting.getParagraphDistance());
        pxParagraphEdgeDistance = LightTool.dip2px(MyApp.getContext(), mSetting.getParagraghEdgeDistance());
        pxPageEdgeDistance = LightTool.dip2px(MyApp.getContext(), mSetting.getPageEdgeDistance());
        pxWidgetHeight = LightTool.dip2px(MyApp.getContext(), mSetting.widgetHeight);

        // calc general var
        try {
            if (mSetting.getUseCustomFont())
                typeface = Typeface.createFromFile(mSetting.getCustomFontPath()); // custom font
        } catch (Exception e) {
            Toast.makeText(MyApp.getContext(), e.toString() + "\n可能的原因有：字体文件不在内置SD卡；内存太小字体太大，请使用简体中文字体，而不是CJK或GBK，谢谢，此功能为试验性功能；", Toast.LENGTH_SHORT).show();
        }
        textPaint = new TextPaint();
        textPaint.setColor(getInDayMode() ? mSetting.fontColorDark : mSetting.fontColorLight);
        textPaint.setTextSize(LightTool.sp2px(MyApp.getContext(), (float) mSetting.getFontSize()));
        if (typeface != null) textPaint.setTypeface(typeface);
        textPaint.setAntiAlias(true);
        fontHeight = (int) textPaint.measureText(sampleText); //(int) textPaint.getTextSize(); // in "px"
        widgetTextPaint = new TextPaint();
        widgetTextPaint.setColor(getInDayMode() ? mSetting.fontColorDark : mSetting.fontColorLight);
        widgetTextPaint.setTextSize(LightTool.sp2px(MyApp.getContext(), (float) mSetting.widgetTextSize));
        widgetTextPaint.setAntiAlias(true);
        widgetFontHeihgt = (int) textPaint.measureText(sampleText);

        // load bitmap
        if (forceMode || !isBackgroundSet) {
            screenSize = LightTool.getRealScreenSize(MyApp.getContext());
            if (Build.VERSION.SDK_INT < 19) {
                screenSize.y -= LightTool.getStatusBarHeightValue(MyApp.getContext());
            }

            if (mSetting.getPageBackgroundType() == WenkuReaderSettingV1.PAGE_BACKGROUND_TYPE.CUSTOM) {
                try {
                    bmBackgroundYellow = BitmapFactory.decodeFile(mSetting.getPageBackgrounCustomPath());
                } catch (OutOfMemoryError oome) {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        bmBackgroundYellow = BitmapFactory.decodeFile(mSetting.getPageBackgrounCustomPath(), options);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
                bmdBackground = null;
            }
            if (mSetting.getPageBackgroundType() == WenkuReaderSettingV1.PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT || bmBackgroundYellow == null) {
                // use system default
                bmBackgroundYellow = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.mipmap.reader_bg_yellow_edge);
                bmTextureYellow = new Bitmap[3];
                bmTextureYellow[0] = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.mipmap.reader_bg_yellow1);
                bmTextureYellow[1] = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.mipmap.reader_bg_yellow2);
                bmTextureYellow[2] = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.mipmap.reader_bg_yellow3);

                bmdBackground = new BitmapDrawable(MyApp.getContext().getResources(), bmTextureYellow[random.nextInt(bmTextureYellow.length)]);
                bmdBackground.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                bmdBackground.setBounds(0, 0, screenSize.x, screenSize.y);
            }
            isBackgroundSet = true;
        }
    }

    /**
     * Reset text color, to fit day/night mode.
     * If textPaint is null, then do nothing.
     */
    static public void resetTextColor() {
        textPaint.setColor(getInDayMode() ? mSetting.fontColorDark : mSetting.fontColorLight);
        widgetTextPaint.setColor(getInDayMode() ? mSetting.fontColorDark : mSetting.fontColorLight);
    }

    /**
     * This function init the view class。
     * Notice: (-1, -1), (-1, 0), (0, -1) means first page.
     *
     * @param context          current context, should be WenkuReaderActivity
     * @param lineIndex        if FORWARDS, this is the last index of last page;
     *                         if CURRENT, this is the first index of this page;
     *                         if BACKWARDS, this is the first index of last page;
     * @param directionForward get next or get previous
     */
    public WenkuReaderPageView(Context context, int lineIndex, int wordIndex, LOADING_DIRECTION directionForward) {
        super(context);
        Log.e("MewX", "-- view: construct my");
        lineInfoList = new ArrayList<>();
        bitmapInfoList = new ArrayList<>();
        mLoader.setCurrentIndex(lineIndex);

        // get environmental vars, use actual layout size
        textAreaSize = new Point(screenSize.x - 2 * (pxPageEdgeDistance + pxParagraphEdgeDistance),
                screenSize.y - 2 * (pxPageEdgeDistance + pxWidgetHeight));
        if (Build.VERSION.SDK_INT < 19) textAreaSize.y = textAreaSize.y + pxWidgetHeight;

        // save vars, calc all ints
        switch (directionForward) {
            case FORWARDS:
                if (wordIndex + 1 < mLoader.getCurrentAsString().length()) {
                    firstLineIndex = lineIndex;
                    if (lineIndex == 0 && wordIndex == 0)
                        firstWordIndex = 0;
                    else
                        firstWordIndex = wordIndex + 1;
                } else if (lineIndex + 1 < mLoader.getElementCount()) {
                    firstLineIndex = lineIndex + 1;
                    firstWordIndex = 0;
                } else {
                    Log.e("MewX", "-- view: end construct A, just return");
                    return;
                }
                mLoader.setCurrentIndex(firstLineIndex);
                calcFromFirst();
                break;

            case CURRENT:
                firstLineIndex = lineIndex;
                firstWordIndex = wordIndex;
                mLoader.setCurrentIndex(firstLineIndex);
                calcFromFirst();
                break;

            case BACKWARDS:
                // fit first and last
                if (wordIndex > 0) {
                    lastLineIndex = lineIndex;
                    lastWordIndex = wordIndex - 1;
                } else if (lineIndex > 0) {
                    lastLineIndex = lineIndex - 1;
                    lastWordIndex = mLoader.getStringLength(lastLineIndex) - 1;
                }

                // firstLineIndex firstWordIndex; and last values changeable
                mLoader.setCurrentIndex(lastLineIndex);
                calcFromLast();
                break;
        }

        for (LineInfo li : lineInfoList)
            Log.e("MewX", "get: " + li.text);

    }

    /**
     * Calc page from first to last.
     * firstLineIndex & firstWordIndex set.
     */
    private void calcFromFirst() {
        int widthSum = 0;
        int heightSum = fontHeight;
        String tempText = "";

        Log.e("MewX", "firstLineIndex = " + firstLineIndex + "; firstWordIndex = " + firstWordIndex);
        for (int curLineIndex = firstLineIndex, curWordIndex = firstWordIndex; curLineIndex < mLoader.getElementCount(); ) {
            // init paragraph head vars
            if (curWordIndex == 0 && mLoader.getCurrentType() == WenkuReaderLoader.ElementType.TEXT) {
                // leading space
                widthSum = 2 * fontHeight;
                tempText = "　　";
            } else if (mLoader.getCurrentType() == WenkuReaderLoader.ElementType.IMAGE_DEPENDENT) {
                if (lineInfoList.size() != 0) {
                    // end a page first
                    lastLineIndex = mLoader.getCurrentIndex() - 1;
                    mLoader.setCurrentIndex(lastLineIndex);
                    lastWordIndex = mLoader.getCurrentAsString().length() - 1;
                    break;
                }

                // one image on page
                lastLineIndex = firstLineIndex = mLoader.getCurrentIndex();
                firstWordIndex = 0;
                lastWordIndex = mLoader.getCurrentAsString().length() - 1;
                LineInfo li = new LineInfo();
                li.type = WenkuReaderLoader.ElementType.IMAGE_DEPENDENT;
                li.text = mLoader.getCurrentAsString();
                lineInfoList.add(li);
                break;
            }

            // get a record of line
            if (mLoader.getCurrentAsString() == null || mLoader.getCurrentAsString().length() == 0) {
                Log.e("MewX", "empty string! in " + curLineIndex + "(" + curWordIndex + ")");
                curWordIndex = 0;
                if (curLineIndex >= mLoader.getElementCount()) {
                    // out of bounds
                    break;
                }
                mLoader.setCurrentIndex(++curLineIndex);
            }
            WenkuReaderLoader.ElementType type = mLoader.getCurrentType();
            String temp = mLoader.getCurrentAsString().charAt(curWordIndex) + "";
            int tempWidth = (int) textPaint.measureText(temp);

            // Line full?
            if (widthSum + tempWidth > textAreaSize.x) {
                // wrap line, save line
                LineInfo li = new LineInfo();
                li.type = WenkuReaderLoader.ElementType.TEXT;
                li.text = tempText;
                lineInfoList.add(li);
                heightSum += pxLineDistance;

                // change vars for next line
                if (heightSum + fontHeight > textAreaSize.y) {
                    // reverse one index
                    if (curWordIndex > 0) {
                        lastLineIndex = curLineIndex;
                        lastWordIndex = curWordIndex - 1;
                    } else if (curLineIndex > 0) {
                        mLoader.setCurrentIndex(--curLineIndex);
                        lastLineIndex = curLineIndex;
                        lastWordIndex = mLoader.getCurrentAsString().length() - 1;
                    } else {
                        lastLineIndex = lastWordIndex = 0;
                    }
                    break; // height overflow
                }

                // height acceptable
                tempText = temp;
                widthSum = tempWidth;
                heightSum += fontHeight;
            } else {
                tempText = tempText + temp;
                widthSum += tempWidth;
            }

            // String end?
            if (curWordIndex + 1 >= mLoader.getCurrentAsString().length()) {
                // next paragraph, wrap line
                LineInfo li = new LineInfo();
                li.type = WenkuReaderLoader.ElementType.TEXT;
                li.text = tempText;
                lineInfoList.add(li);
                heightSum += pxParagraphDistance;

                // height not acceptable
                if (heightSum + fontHeight > textAreaSize.y) {
                    lastLineIndex = mLoader.getCurrentIndex();
                    lastWordIndex = mLoader.getCurrentAsString().length() - 1;
                    break; // height overflow
                }

                // height acceptable
                heightSum += fontHeight;
                widthSum = 0;
                tempText = "";
                curWordIndex = 0;
                if (curLineIndex + 1 >= mLoader.getElementCount()) {
                    // out of bounds
                    lastLineIndex = curLineIndex;
                    lastWordIndex = mLoader.getCurrentAsString().length() - 1;
                    break;
                }
                mLoader.setCurrentIndex(++curLineIndex);
            } else {
                curWordIndex++;
            }
        }
    }

    /**
     * Calc page from last to first
     * lastLineIndex & lastWordIndex set.
     */
    private void calcFromLast() {

        int heightSum = 0;
        boolean isFirst = true;
        mLoader.setCurrentIndex(lastLineIndex);

        LineLoop:
        for (int curLineIndex = lastLineIndex, curWordIndex = lastWordIndex; curLineIndex >= 0; ) {
            // calc curLine to curWord(contained), make a String list
            WenkuReaderLoader.ElementType curType = mLoader.getCurrentType();
            String curString = mLoader.getCurrentAsString();

            // special to image
            if (curType == WenkuReaderLoader.ElementType.IMAGE_DEPENDENT && lineInfoList.size() != 0) {
                Log.e("MewX", "jump 1");
                firstLineIndex = curLineIndex + 1;
                firstWordIndex = 0;
                mLoader.setCurrentIndex(firstLineIndex);
                lineInfoList = new ArrayList<>();
                calcFromFirst();
                break;
            } else if (curType == WenkuReaderLoader.ElementType.IMAGE_DEPENDENT) {
                // one image on page
                lastLineIndex = firstLineIndex = mLoader.getCurrentIndex();
                firstWordIndex = 0;
                lastWordIndex = mLoader.getCurrentAsString().length() - 1;
                LineInfo li = new LineInfo();
                li.type = WenkuReaderLoader.ElementType.IMAGE_DEPENDENT;
                li.text = mLoader.getCurrentAsString();
                lineInfoList.add(li);
                break;
            }

            int tempWidth = 0;
            List<LineInfo> curList = new ArrayList<>();
            String temp = "";
            for (int i = 0; i < curString.length(); ) {
                if (i == 0) {
                    tempWidth += fontHeight + fontHeight;
                    temp = "　　";
                }

                String c = curString.charAt(i) + "";
                int width = (int) textPaint.measureText(c);
                if (tempWidth + width > textAreaSize.x) {
                    // save line to next
                    LineInfo li = new LineInfo();
                    li.type = WenkuReaderLoader.ElementType.TEXT;
                    li.text = temp;
                    curList.add(li);

                    // fit needs
                    if (i >= curWordIndex) break;

                    // goto next round
                    tempWidth = 0;
                    temp = "";
                    continue;
                } else {
                    temp = temp + c;
                    tempWidth += width;
                    i++;
                }

                // string end
                if (i == curString.length()) {
                    LineInfo li = new LineInfo();
                    li.type = WenkuReaderLoader.ElementType.TEXT;
                    li.text = temp;
                    curList.add(li);
                }
            }

            // reverse to add to lineInfoList, full to break, image to do calcFromFirst then break
            for (int i = curList.size() - 1; i >= 0; i--) {
                if (isFirst)
                    isFirst = false;
                else if (i == curList.size() - 1)
                    heightSum += pxParagraphDistance;
                else
                    heightSum += pxLineDistance;

                heightSum += fontHeight;
                if (heightSum > textAreaSize.y) {
                    // calc first index
                    int indexCount = -2;
                    for (int j = 0; j <= i; j++) indexCount += curList.get(j).text.length();
                    firstLineIndex = curLineIndex;
                    firstWordIndex = indexCount + 1;

                    // out of index
                    if (firstWordIndex + 1 >= curString.length()) {
                        firstLineIndex = curLineIndex + 1;
                        firstWordIndex = 0;
                    }
                    break LineLoop;
                }
                lineInfoList.add(0, curList.get(i));
            }
            for (LineInfo li : lineInfoList)
                Log.e("MewX", "full: " + li.text);

            // not full to continue, set curWord as last index of the string
            if (curLineIndex - 1 >= 0) {
                mLoader.setCurrentIndex(--curLineIndex);
                curWordIndex = mLoader.getCurrentAsString().length();
            } else {
                Log.e("MewX", "jump 2");
                firstLineIndex = 0;
                firstWordIndex = 0;
                mLoader.setCurrentIndex(firstLineIndex);
                lineInfoList = new ArrayList<>();
                calcFromFirst();
                break;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawLine(0.0f, 0.0f, 320.0f, 320.0f, new Paint()); // px
        if (mSetting == null || mLoader == null) return;
        Log.e("MewX", "onDraw()");

        // draw background
        if (getInDayMode()) {
            // day
            if (bmdBackground != null)
                bmdBackground.draw(canvas);
            if (bmBackgroundYellow.getWidth() != screenSize.x || bmBackgroundYellow.getHeight() != screenSize.y)
                bmBackgroundYellow = Bitmap.createScaledBitmap(bmBackgroundYellow, screenSize.x, screenSize.y, true);
            canvas.drawBitmap(bmBackgroundYellow, 0, 0, null);

        } else {
            // night
            Paint paintBackground = new Paint();
            paintBackground.setColor(mSetting.bgColorDark);
            canvas.drawRect(0, 0, screenSize.x, screenSize.y, paintBackground);
        }
//        Paint paintBackground = new Paint();
//        paintBackground.setColor(mSetting.inDayMode ? mSetting.bgColorLight : mSetting.bgColorDark);
//        canvas.drawRect(0, 0, screenSize.x, screenSize.y, paintBackground);

        // draw divider
//        Paint paintDivider = new Paint();
//        paintDivider.setColor(getContext().getResources().getColor(mSetting.inDayMode ? R.color.dlgDividerColor : R.color.reader_default_text_light));
//        canvas.drawLine(1, 1, 1, screenSize.y - 1, paintDivider);
//        canvas.drawLine(screenSize.x - 1, 1, screenSize.x - 1, screenSize.y - 1, paintDivider);

        // draw widgets
        canvas.drawText(mLoader.getChapterName(), pxPageEdgeDistance, screenSize.y - pxPageEdgeDistance, widgetTextPaint);
        String percentage = "( " + (lastLineIndex + 1) * 100 / mLoader.getElementCount() + "% )";
        int tempWidth = (int) widgetTextPaint.measureText(percentage);
        canvas.drawText(percentage, screenSize.x - pxPageEdgeDistance - tempWidth, screenSize.y - pxPageEdgeDistance, widgetTextPaint);

        // draw text on average in page and line
        int heightSum = fontHeight + pxPageEdgeDistance + pxWidgetHeight;
        if (Build.VERSION.SDK_INT < 19) heightSum -= pxWidgetHeight;
        for (int i = 0; i < lineInfoList.size(); i++) {
            LineInfo li = lineInfoList.get(i);
            if (i != 0) {
                if (li.text.length() > 2 && li.text.substring(0, 2).equals("　　")) {
                    heightSum += pxParagraphDistance;
                } else {
                    heightSum += pxLineDistance;
                }
            }

            Log.e("MewX", "draw: " + li.text);
            if (li.type == WenkuReaderLoader.ElementType.TEXT) {
                canvas.drawText(li.text, (float) (pxPageEdgeDistance + pxParagraphEdgeDistance), (float) heightSum, textPaint);
                heightSum += fontHeight;
            } else if (li.type == WenkuReaderLoader.ElementType.IMAGE_DEPENDENT) {
                if (bitmapInfoList != null) {
                    int foundIndex = -1;
                    for (BitmapInfo bi : bitmapInfoList) {
                        if (bi.idxLineInfo == i) {
                            foundIndex = bitmapInfoList.indexOf(bi);
                            break;
                        }
                    }

                    if (foundIndex == -1) {
                        // not found, new load task
                        canvas.drawText("正在加载图片：" + li.text.substring(21, li.text.length()), (float) (pxPageEdgeDistance + pxParagraphEdgeDistance), (float) heightSum, textPaint);
                        BitmapInfo bitmapInfo = new BitmapInfo();
                        bitmapInfo.idxLineInfo = i;
                        bitmapInfo.x_beg = pxPageEdgeDistance + pxParagraphEdgeDistance;
                        bitmapInfo.y_beg = pxPageEdgeDistance + pxWidgetHeight;
                        if (Build.VERSION.SDK_INT < 19) bitmapInfo.y_beg -= pxWidgetHeight;
                        bitmapInfo.height = textAreaSize.y;
                        bitmapInfo.width = textAreaSize.x;
                        bitmapInfoList.add(0, bitmapInfo);
                        bi_bak = bitmapInfoList.get(0);
                        ExecutorService threadPool = SingletonThreadPool.getInstance();
                        threadPool.execute(new AsyncLoadImage());

                    } else {
                        if (bitmapInfoList.get(foundIndex).bm == null) {
                            canvas.drawText("正在加载图片：" + li.text.substring(21, li.text.length()), (float) (pxPageEdgeDistance + pxParagraphEdgeDistance), (float) heightSum, textPaint);
                        } else {
//                            canvas.drawText("Can you see image?", (float) (pxPageEdgeDistance + pxParagraphEdgeDistance), (float) heightSum, textPaint);
                            canvas.drawBitmap(bitmapInfoList.get(foundIndex).bm, bitmapInfoList.get(foundIndex).x_beg, bitmapInfoList.get(foundIndex).y_beg, new Paint());
                        }
                    }
                } else {
                    canvas.drawText("Unexpected array: " + li.text.substring(21, li.text.length()), (float) (pxPageEdgeDistance + pxParagraphEdgeDistance), (float) heightSum, textPaint);
                }
            } else {
                canvas.drawText("（！请先用旧引擎浏览）图片" + li.text.substring(21, li.text.length()), (float) (pxPageEdgeDistance + pxParagraphEdgeDistance), (float) heightSum, textPaint);
            }
        }
    }

    public int getFirstLineIndex() {
        return firstLineIndex;
    }

    public int getFirstWordIndex() {
        return firstWordIndex;
    }

    public int getLastLineIndex() {
        return lastLineIndex;
    }

    public int getLastWordIndex() {
        return lastWordIndex;
    }

    private static final int PIC_OK = 0;

    private BitmapInfo bi_bak;
    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PIC_OK:
                    WenkuReaderPageView.this.postInvalidate();
                    break;

            }
        }
    };

    class AsyncLoadImage implements Runnable {

        @Override
        public void run() {
            AsyncLoadImage();
        }
    }

    public void AsyncLoadImage() {

        String imgFileName = GlobalConfig.generateImageFileNameByURL(lineInfoList.get(bi_bak.idxLineInfo).text);
        if (GlobalConfig.getAvailableNovolContentImagePath(imgFileName) == null) {
            if (!GlobalConfig.saveNovelContentImage(lineInfoList.get(bi_bak.idxLineInfo).text)) {

                Toast.makeText(getContext(), Wenku8Error.ErrorCode.NETWORK_ERROR.toString(), Toast.LENGTH_SHORT).show();
            }
            imgFileName = GlobalConfig.generateImageFileNameByURL(lineInfoList.get(bi_bak.idxLineInfo).text);
        }
        setDataSubscriber(getContext(), Uri.parse("file://" + GlobalConfig.getAvailableNovolContentImagePath(imgFileName)), bi_bak.width, bi_bak.height);


    }


    public void setDataSubscriber(Context context, Uri uri, int width, int height) {
        DataSubscriber dataSubscriber = new BaseDataSubscriber<CloseableReference<CloseableBitmap>>() {
            @Override
            public void onNewResultImpl(
                    DataSource<CloseableReference<CloseableBitmap>> dataSource) {
                if (!dataSource.isFinished()) {
                    return;
                }
                CloseableReference<CloseableBitmap> imageReference = dataSource.getResult();
                if (imageReference != null) {
                    final CloseableReference<CloseableBitmap> closeableReference = imageReference.clone();
                    try {
                        CloseableBitmap closeableBitmap = closeableReference.get();
                        Bitmap bitmap = closeableBitmap.getUnderlyingBitmap();
                        if (bitmap != null && !bitmap.isRecycled()) {
                            //you can use bitmap here
                            Log.e("bmp!!!!", "hase!!!!!");
                            bi_bak.bm = bitmap;
                            int width = bi_bak.bm.getWidth(), height = bi_bak.bm.getHeight();
                            if (bi_bak.height / (float) bi_bak.width > height / (float) width) {
                                // fit width
                                float percentage = (float) height / width;
                                bi_bak.height = (int) (bi_bak.width * percentage);
                            } else {
                                // fit height
                                float percentage = (float) width / height;
                                bi_bak.width = (int) (bi_bak.height * percentage);
                            }
                            bi_bak.bm = Bitmap.createScaledBitmap(bi_bak.bm, bi_bak.width, bi_bak.height, true);
                            Message msg = new Message();
                            msg.what = PIC_OK;
                            handler.sendMessage(msg);
                        }
                    } finally {
                        imageReference.close();
                        closeableReference.close();
                    }
                }
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                Throwable throwable = dataSource.getFailureCause();
                // handle failure
            }
        };
        getBitmap(context, uri, width, height, dataSubscriber);
    }

    /**
     * @param context
     * @param uri
     * @param width
     * @param height
     * @param dataSubscriber
     */
    public void getBitmap(Context context, Uri uri, int width, int height, DataSubscriber dataSubscriber) {
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
        if (width > 0 && height > 0) {
            builder.setResizeOptions(new ResizeOptions(width, height));
        }
        ImageRequest request = builder.build();
        DataSource<CloseableReference<CloseableImage>>
                dataSource = imagePipeline.fetchDecodedImage(request, context);
        dataSource.subscribe(dataSubscriber, UiThreadImmediateExecutorService.getInstance());
    }


    public void watchImageDetailed(Activity activity) {
        if (bitmapInfoList == null || bitmapInfoList.size() == 0 || bitmapInfoList.get(0).bm == null) {
            Toast.makeText(getContext(), getResources().getString(R.string.reader_view_image_no_image), Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(activity, ViewImageDetailActivity.class);
            intent.putExtra("path", GlobalConfig.getAvailableNovolContentImagePath(GlobalConfig.generateImageFileNameByURL(lineInfoList.get(bitmapInfoList.get(0).idxLineInfo).text)));
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.fade_in, R.anim.hold); // fade in animation
        }
    }
}
