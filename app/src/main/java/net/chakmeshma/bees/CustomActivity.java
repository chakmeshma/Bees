package net.chakmeshma.bees;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.chakmeshma.brutengine.Engine;
import net.chakmeshma.brutengine.android.GameActivity;
import net.chakmeshma.brutengine.android.GameRenderer;
import net.chakmeshma.brutengine.android.SurfaceView;
import net.chakmeshma.brutengine.development.DebugUtilities;
import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.development.exceptions.InvalidStackOperationException;
import net.chakmeshma.brutengine.mathematics.Camera;
import net.chakmeshma.brutengine.mathematics.Transform;
import net.chakmeshma.brutengine.rendering.Mesh;
import net.chakmeshma.brutengine.rendering.Program;
import net.chakmeshma.brutengine.rendering.Renderable;
import net.chakmeshma.brutengine.rendering.StepLoadListener;
import net.chakmeshma.brutengine.rendering.VariableReferenceable;
import net.chakmeshma.brutengine.utilities.GeneralUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_DITHER;
import static android.opengl.GLES20.GL_FUNC_ADD;
import static android.opengl.GLES20.GL_LESS;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.glBlendEquation;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFlush;
import static android.widget.RelativeLayout.CENTER_IN_PARENT;
import static android.widget.RelativeLayout.TRUE;
import static net.chakmeshma.brutengine.development.DebugUtilities.FramerateCapture.getCurrentStackSize;
import static net.chakmeshma.brutengine.development.DebugUtilities.FramerateCapture.popTimestampsAll;

public class CustomActivity extends AppCompatActivity implements GameActivity {
    //region fields
    private static Point clientSize = null;
    private final String MESSAGE_UPDATE_FRAMERATE_TEXT_DATA_KEY = "net.chakmeshma.bees.FRAMERATE_TEXT";
    private final int prcChartPadding = 0;
    private int LOAD_PART_COUNT = 1;
    private volatile int numGLFlushes = 0;
    private Handler remoteUIThreadHandler;
    private Handler localUIThreadHandler;
    private long[] chartBuffer;
    private AppCompatTextView debugTextView;
    private AppCompatTextView debug2TextView;
    private SurfaceView surfaceView;
    private GameRenderer renderer;
    private ViewGroup root;
    private ViewGroup loadingFrame;
    private ProgressBar loadingProgressBar;
    private LineChart chart;
    private long msBackDoublePressTriggerTime = 300;
    private long lastBackPressedTimestamp = 0;
    private boolean initializationStarted = false;
    private boolean initializationEnded = false;
    private long lastBackDoublePressedTimestamp = 0;
    private boolean _chartActive = false;
    private boolean _chartLoaded = false;
    private volatile List<Entry> maxChartEntries;
    private volatile List<Entry> meanChartEntries;
    private volatile List<Entry> minChartEntries;
    private float lastChartX = 0.0f;
    //endregion

    //region dimensions and sizes
    static int getHorizontalSize() {
        return clientSize.y;
    }

    static int getSmallerSize() {
        return Math.min(clientSize.x, clientSize.y);
    }

    static int getBiggerSize() {
        return Math.max(clientSize.x, clientSize.y);
    }

    static int getSBRatio() {
        return getSmallerSize() / getBiggerSize();
    }

    static int getBSRatio() {
        return getBiggerSize() / getSmallerSize();
    }

    static float getVHRatio() {
        return clientSize.x / clientSize.y;
    }

    static float getHVRatio() {
        return clientSize.y / clientSize.x;
    }

    static float getScaleReferenceNumber() {
        return getSmallerSize();
    }

    static int getVerticalSize() {
        return clientSize.x;
    }

    //region sendMessageToUIThreadHandler
    @Override
    public void sendMessageToUIThreadHandler(UIThreadMessageType what, int arg1, int arg2) {
        if (remoteUIThreadHandler != null) {
            Message message = Message.obtain();

            message.what = what.ordinal();
            message.arg1 = arg1;
            message.arg2 = arg2;

            remoteUIThreadHandler.sendMessage(message);
        } else
            throw new RuntimeException("UI thread handler not set");
    }
    //endregion

    @Override
    public void sendMessageToUIThreadHandler(UIThreadMessageType what, int arg1) {
        sendMessageToUIThreadHandler(what, arg1, 0);
    }

    @Override
    public void sendMessageToUIThreadHandler(UIThreadMessageType what) {
        sendMessageToUIThreadHandler(what, 0, 0);
    }

    private void sendMessageToUIThreadHandler(ActivityMessageType what, int arg1, int arg2) {
        if (localUIThreadHandler != null) {
            Message message = Message.obtain();

            message.what = what.ordinal();
            message.arg1 = arg1;
            message.arg2 = arg2;

            localUIThreadHandler.sendMessage(message);
        } else
            throw new RuntimeException("UI thread handler not set");
    }

    private void sendMessageToUIThreadHandler(ActivityMessageType what, int arg1) {
        sendMessageToUIThreadHandler(what, arg1, 0);
    }

    private void sendMessageToUIThreadHandler(ActivityMessageType what) {
        sendMessageToUIThreadHandler(what, 0, 0);
    }

    private void sendMessageToUIThreadHandler(ActivityMessageType what, Object[] data) {
        if (localUIThreadHandler != null) {
            Message message = Message.obtain();

            message.what = what.ordinal();

            Bundle dataBundle = new Bundle();

            String messageText;

            switch (what) {
                case MESSAGE_UPDATE_FRAMERATE_TEXT:
                    messageText = String.format("%d\n%d\n%d", data[0], data[1], data[2]);

                    dataBundle.putString(MESSAGE_UPDATE_FRAMERATE_TEXT_DATA_KEY, messageText);
                    break;
                case MESSAGE_UPDATE_TOTAL_FRAMERATE_TEXT:
                    messageText = String.format("%d\n%d\n%d", data[0], data[1], data[2]);

                    dataBundle.putString(MESSAGE_UPDATE_FRAMERATE_TEXT_DATA_KEY, messageText);
                    break;
                default:
                    DebugUtilities.logWarning("undefined message type ('what' parameter) to UI Thread Handler\nunknown data parameters interpretation based on message type\nwill send data-empty message with correct message type");
            }

            message.setData(dataBundle);

            localUIThreadHandler.sendMessage(message);
        } else
            throw new RuntimeException("UI thread handler not set");
    }

    @Override
    public void incrementCountGLFlushes() {
        numGLFlushes++;
    }
    //endregion

    //region initialization
    private void initUIThreadHandlers() {
        remoteUIThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == UIThreadMessageType.MESSAGE_PART_LOADED.ordinal()) {
                    int currentProgress = loadingProgressBar.getProgress();
                    currentProgress += 1;
                    loadingProgressBar.setProgress(currentProgress);
                    //root.invalidate();

                    if (currentProgress == LOAD_PART_COUNT)
                        sendMessageToUIThreadHandler(ActivityMessageType.MESSAGE_COMPLETE_LOADED);
                } else if (msg.what == UIThreadMessageType.MESSAGE_EXTEND_LOAD_PARTS_COUNT.ordinal()) {
                    int extraPartCount = msg.arg1;

                    if (msg.arg1 > 0) {
                        LOAD_PART_COUNT += extraPartCount;
                        loadingProgressBar.setMax(LOAD_PART_COUNT);
                    }

                }
            }
        };

        localUIThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ActivityMessageType.MESSAGE_UPDATE_FRAMERATE_TEXT.ordinal()) {
                    updateFramerateText(msg.getData().getString(MESSAGE_UPDATE_FRAMERATE_TEXT_DATA_KEY));
                } else if (msg.what == ActivityMessageType.MESSAGE_UPDATE_TOTAL_FRAMERATE_TEXT.ordinal()) {
                    updateTotalFramerateText(msg.getData().getString(MESSAGE_UPDATE_FRAMERATE_TEXT_DATA_KEY));
                } else if (msg.what == ActivityMessageType.MESSAGE_COMPLETE_LOADED.ordinal()) {
                    onFinishedLoaded();

                } else if (msg.what == ActivityMessageType.MESSAGE_BEGIN_INITIALIZATION.ordinal()) {
                    try {
                        if (initializationStarted)
                            throw new InitializationException("initialization already started!");

                        init();
                    } catch (InitializationException e) {
                        throw new RuntimeException(e);
                    }

                } else if (msg.what == ActivityMessageType.MESSAGE_CLEAR_CHART_ENTRIES.ordinal()) {
                    clearChart();

                } else if (msg.what == ActivityMessageType.MESSAGE_UPDATE_CHART.ordinal()) {
                    if (chartBuffer != null) {
                        updateChart(chartBuffer);
                        chartBuffer = null;
                    }
                }
            }

            private void updateFramerateText(String formattedText) {
                debugTextView.setText(formattedText);
            }

            private void updateTotalFramerateText(String formattedText) {
                debug2TextView.setText(formattedText);
            }

            private void onFinishedLoaded() {
                root.removeView(loadingFrame);
                loadingFrame = null;
                initializationEnded = true;
            }
        };
    }

    private void initSizes() {
        Display display = getWindowManager().getDefaultDisplay();
        clientSize = new Point();
        display.getSize(clientSize);
    }

    private void init() throws InitializationException {
        Engine.initContext(this);

        initializationStarted = true;

        root = (ViewGroup) findViewById(R.id.main_relative_layout);
        loadingFrame = (ViewGroup) findViewById(R.id.loading_relative_layout);
        loadingProgressBar = (ProgressBar) findViewById(R.id.loading_progressbar);
        loadingProgressBar.setMax(LOAD_PART_COUNT);
        debugTextView = (AppCompatTextView) findViewById(R.id.debug_textview);
        debug2TextView = (AppCompatTextView) findViewById(R.id.debug2_textview);

        initLoadingWaiterThread().start();
        initDebugThread().start();

        GameRenderer gameRenderer = new GameRenderer() {
            @Override
            public void initState() {
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);

                glEnable(GL_BLEND);
                glBlendEquation(GL_FUNC_ADD);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LESS);

                glDisable(GL_DITHER);

                glFlush();
            }

            @Override
            protected void initDrawables() throws InitializationException {
                List<Renderable.SimpleRenderable> renderables = new ArrayList<>();

                //region program setup
                Map<Program.DefinedUniformType, VariableReferenceable.VariableMatcher> definedUniforms = new EnumMap<>(Program.DefinedUniformType.class);
                definedUniforms.put(Program.DefinedUniformType.MODEL_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat4", "modelMatrix"));
                definedUniforms.put(Program.DefinedUniformType.VIEW_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat4", "viewMatrix"));
                definedUniforms.put(Program.DefinedUniformType.PROJECTION_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat4", "projectionMatrix"));
                definedUniforms.put(Program.DefinedUniformType.ROTATION_MATRIX_UNIFORM, new VariableReferenceable.VariableMatcher.EqualityMatcher("mat3", "rotationMatrix"));

                Map<Program.DefinedAttributeType, VariableReferenceable.VariableMatcher> definedAttributes = new EnumMap<>(Program.DefinedAttributeType.class);
                definedAttributes.put(Program.DefinedAttributeType.POSITION_ATTRIBUTE, new VariableReferenceable.VariableMatcher.EqualityMatcher("vec3", "positions"));
                definedAttributes.put(Program.DefinedAttributeType.NORMAL_ATTRIBUTE, new VariableReferenceable.VariableMatcher.EqualityMatcher("vec3", "normals"));

                Program phongProgram = new Program("phong.vert", "phong.frag", definedUniforms, definedAttributes);
                //endregion

                float[] cameraFocusPoint = new float[]{0.0f, 0.0f, 0.0f};

                //region theCamera setup
                theCamera = new Camera(
                        cameraFocusPoint[0],    // focusX
                        cameraFocusPoint[1],    // focusY
                        cameraFocusPoint[2],    // focusZ
                        100.0f,                 // distance
                        1.0f,                   // near
                        10000.0f,               // far
                        100.0f,                 // fovy
                        0.0f,                   // rotation yaw
                        0.0f,                   // rotation pitch
                        300,                    // viewport width
                        300);                   // viewport height
                //endregion

                //region mesh setup
                StepLoadListener meshStepLoadListener = new StepLoadListener() {
                    @Override
                    public void setPartCount(int partCount) {
                        Engine.context.sendMessageToUIThreadHandler(GameActivity.UIThreadMessageType.MESSAGE_EXTEND_LOAD_PARTS_COUNT, partCount);
                    }

                    @Override
                    public void partLoaded() {
                        Engine.context.sendMessageToUIThreadHandler(GameActivity.UIThreadMessageType.MESSAGE_PART_LOADED);
                    }
                };

//                Mesh hexahiveMesh = new Mesh(new Mesh.ObjFile("beehive.obj"), meshStepLoadListener);
                Mesh sphereMarker = new Mesh(new Mesh.ObjFile("ico.obj"), meshStepLoadListener);
                //endregion

                for (int i = 0; i < 512; i++) {
                    Random random = new Random(System.nanoTime());

                    float x = (random.nextFloat() * 100.0f) - 50.0f;   /////////////////////
                    float y = (random.nextFloat() * 100.0f) - 50.0f;   ///VERTEILUNG VCTR///
                    float z = (random.nextFloat() * 100.0f) - 50.0f;   /////////////////////

                    renderables.add(new Renderable.SimpleRenderable(phongProgram, sphereMarker, new Transform(
                            x, y, z,                     //////////////////
                            0.0f, 0.0f, 0.0f,            ///TRNSFRM MTRX///
                            0.0f, 0.0f, 0.0f),           //////////////////
                            theCamera));
                }

                renderableGroup = new Renderable.SimpleSharedCameraGroupRenderable(renderables);
            }
        };

        this.renderer = gameRenderer;

        surfaceView = new SurfaceView(gameRenderer);

        RelativeLayout.LayoutParams surfaceViewLayoutParams;
        surfaceViewLayoutParams = new RelativeLayout.LayoutParams(getVerticalSize(), getHorizontalSize());
        surfaceViewLayoutParams.addRule(CENTER_IN_PARENT, TRUE);
        surfaceView.setLayoutParams(surfaceViewLayoutParams);
        root.addView(surfaceView, 0);
    }

    private Thread initLoadingWaiterThread() {
        return new Thread(new Runnable() {
            long msRefreshStateInterval = 30L;

            @Override
            public void run() {
                while (numGLFlushes == 0) {
                    try {
                        Thread.sleep(msRefreshStateInterval);
                    } catch (InterruptedException e) {

                    }
                }

                sendMessageToUIThreadHandler(UIThreadMessageType.MESSAGE_PART_LOADED);

            }
        }, "first glFlush waiter thread");
    }

    private Thread initDebugThread() {
        Thread debugThread = new Thread(new Runnable() {
            long msRefreshFramerateTextInterval = 1000L;                                            //
            long totalMean, totalMin, totalMax, totalSum, totalCount = 0;                           // SAFE TO DO BECAUSE THE THREAD IS NOT USABLE AFTER TERMINATION (AND GETS GCOLLECTED)

            @Override
            public void run() {
                while (true) {
                    //region [dumping the whole stack and saving it in chartBuffer (not synchronized)]
                    try {
                        Thread.sleep(msRefreshFramerateTextInterval);
                    } catch (InterruptedException e) {

                    }

                    if (getCurrentStackSize() < 2)
                        continue;

                    long[] framerates = null;

                    try {
                        framerates = popTimestampsAll(true);
                    } catch (InvalidStackOperationException e) {
                        continue;
                    }

                    if (isChartActive()) {
                        chartBuffer = Arrays.copyOf(framerates, framerates.length);
                        sendMessageToUIThreadHandler(ActivityMessageType.MESSAGE_UPDATE_CHART);
                    }
                    //endregion

                    long min = 0, mean = 0, max = 0, sum = 0;

                    int fpsCount = 0;

                    for (int i = 1; i < framerates.length; i++) {
                        int fps = (int) Math.round(1000_000_000.0 / ((double) (framerates[i] - framerates[i - 1])));

                        if (i == 1) {
                            min = fps;
                            max = fps;
                            sum = fps;

                            fpsCount++;
                        } else {
                            if (fps < min)
                                min = fps;
                            if (fps > max)
                                max = fps;
                            sum += fps;

                            fpsCount++;
                        }
                    }

                    mean = Math.round(((double) sum) / ((double) fpsCount));

                    if (totalCount == 0) {
                        totalCount = fpsCount;
                        totalSum = sum;

                        totalMin = min;
                        totalMean = mean;
                        totalMax = max;
                    } else {
                        totalCount += fpsCount;
                        totalSum += sum;

                        if (min < totalMin)
                            totalMin = min;

                        totalMean = Math.round(((double) totalSum) / ((double) totalCount));

                        if (max > totalMax) {
                            totalMax = max;
                        }
                    }

                    sendMessageToUIThreadHandler(ActivityMessageType.MESSAGE_UPDATE_FRAMERATE_TEXT, new Long[]{max, mean, min});
                    sendMessageToUIThreadHandler(ActivityMessageType.MESSAGE_UPDATE_TOTAL_FRAMERATE_TEXT, new Long[]{totalMax, totalMean, totalMin});
                }

                //return
            }
        }, "loader thread");

        return debugThread;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSizes();
        setContentView(R.layout.main_layout);

        initUIThreadHandlers();

        CustomAppCompatImageView splashImage = (CustomAppCompatImageView) findViewById(R.id.loading_splash_imageview);

        splashImage.setOnNextDrawOnceCallback(new Runnable() {
            @Override
            public void run() {
                sendMessageToUIThreadHandler(ActivityMessageType.MESSAGE_BEGIN_INITIALIZATION);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (renderer != null)
            renderer.pauseRendering();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (renderer != null)
            renderer.resumeRendering();
    }

    @Override
    public void onBackPressed() {
        if (!initializationEnded) {
            super.onBackPressed();

            return;
        }

        onSingleBackPressed();

        if (lastBackPressedTimestamp == 0)
            lastBackPressedTimestamp = System.nanoTime();
        else {
            long currentTimestamp = System.nanoTime();

            if (currentTimestamp - lastBackPressedTimestamp < msBackDoublePressTriggerTime * 1000_000L)
                onDoubleBackPressed();
        }

        lastBackPressedTimestamp = System.nanoTime();
    }

    //region framerates chart
    private void createFrameratesChart(boolean setVisible) {
        int availableWidth, availableHeight;

        availableWidth = getVerticalSize();
        availableHeight = getHorizontalSize();

        int chartWidth, chartHeight;

        int padding = Math.round((((float) prcChartPadding) / 100.0f) * availableWidth);

        chartWidth = availableWidth - padding;
        chartHeight = availableHeight - padding;

        float yAxisMax = 1000.0f / 0.4f;

        chart = new LineChart(CustomActivity.this);
        chart.setHardwareAccelerationEnabled(true);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(chartWidth, chartHeight);
        layoutParams.addRule(CENTER_IN_PARENT, TRUE);
        chart.setLayoutParams(layoutParams);
        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(false);
        YAxis yAxisLeft = chart.getAxisLeft();
        YAxis yAxisRight = chart.getAxisRight();
        yAxisLeft.setAxisMinimum(0.0f);
        yAxisRight.setAxisMinimum(0.0f);
        yAxisLeft.setAxisMaximum(yAxisMax);
        yAxisRight.setAxisMaximum(yAxisMax);
        Description chartDescription = new Description();
        chartDescription.setEnabled(false);

        chart.setVisibleYRange(0.0f, yAxisMax, YAxis.AxisDependency.LEFT);
        chart.setVisibleYRange(0.0f, yAxisMax, YAxis.AxisDependency.RIGHT);

        chart.setDoubleTapToZoomEnabled(false);
        chart.setDescription(chartDescription);
        chart.setNoDataTextColor(Color.parseColor("#FF0000"));

        if (setVisible)
            chart.setVisibility(View.VISIBLE);
        else
            chart.setVisibility(View.GONE);

        chart.setBackgroundResource(R.color.colorFramesChartBackground);

        root.addView(chart, root.indexOfChild(debug2TextView) - 1);
    }

    private boolean isChartActive() {
        return _chartActive;
    }

    private void setChartActive(boolean set) {
        if (set) {
            if (!isChartLoaded()) {
                loadChart(true);
            } else {
                chart.setVisibility(View.VISIBLE);
                this._chartActive = true;
            }
        } else {
            if (!isChartLoaded()) {
                //ignore
            } else {
                this._chartActive = false;
                chart.setVisibility(View.GONE);
            }
        }
    }

    private void clearChart() {
        if (isChartLoaded())
            chart.clear();
        minChartEntries.clear();
        meanChartEntries.clear();
        maxChartEntries.clear();
        lastChartX = 0.0f;
    }

    private void updateChart(long[] frameRates) {
        if (isChartLoaded()) {
            float x0 = (float) (0.0 / 1000_000.0);

            float totalElapsed = 0.0f;
            int counter = 0;

            maxChartEntries.add(new Entry(x0 + this.lastChartX, 0.0f));
            meanChartEntries.add(new Entry(x0 + this.lastChartX, 0.0f));
            minChartEntries.add(new Entry(x0 + this.lastChartX, 0.0f));

            float min = Float.MAX_VALUE;
            float max = 0.0f;

            for (int i = 1; i < frameRates.length; i++) {
                float elapsed = (float) (((double) frameRates[i] - frameRates[i - 1]) / 1000_000.0);
                if (elapsed < min)
                    min = elapsed;

                if (elapsed > max)
                    max = elapsed;

                totalElapsed += elapsed;
                counter++;
            }

            float x1 = (float) (((double) frameRates[frameRates.length - 1] - frameRates[0]) / 1000_000.0);
            float xMean = (x0 + x1) / 2.0f;
            float mean = totalElapsed / counter;

            maxChartEntries.add(new Entry(xMean + this.lastChartX, max));
            meanChartEntries.add(new Entry(xMean + this.lastChartX, mean));
            minChartEntries.add(new Entry(xMean + this.lastChartX, min));


            maxChartEntries.add(new Entry(x1 + this.lastChartX, 0.0f));
            meanChartEntries.add(new Entry(x1 + this.lastChartX, 0.0f));
            minChartEntries.add(new Entry(x1 + this.lastChartX, 0.0f));

            this.lastChartX += x1;

            LineDataSet[] lineDataSets = new LineDataSet[3];

            lineDataSets[0] = new LineDataSet(minChartEntries, "min Frame-Time");
            lineDataSets[1] = new LineDataSet(meanChartEntries, "mean FT");
            lineDataSets[2] = new LineDataSet(maxChartEntries, "max FT");

            int i = 0;

            for (LineDataSet lineDataSet : lineDataSets) {
//                lineDataSet.setCircleColor(Color.parseColor("#000000"));
//                lineDataSet.setCircleRadius(3.0f);

                lineDataSet.setDrawCircles(false);
                //lineDataSet.setDrawCircleHole(false);

                IValueFormatter valueFormatter = new IValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                        if (value == 0.0f)
                            return "";
                        else
                            return String.format("%dfps", Math.round(1000.0f / value));
                    }
                };


//                float dpFontSize = GeneralUtilities.pxToDP(this, 0.01f * getScaleReferenceNumber(), true);
                float dpFontSize = GeneralUtilities.pxToDp(Math.round(0.03f * getScaleReferenceNumber()));

                lineDataSet.setValueFormatter(valueFormatter);

                switch (i) {
                    case 0:
                        lineDataSet.setColors(Color.parseColor("#7F00CC00"));
                        lineDataSet.setValueTextColor(Color.parseColor("#7F00CC00"));
                        lineDataSet.setValueTextSize(dpFontSize / 1.500f);
                        break;
                    case 1:
                        lineDataSet.setColors(Color.parseColor("#BF000066"));
                        lineDataSet.setValueTextColor(Color.parseColor("#BF000066"));
                        lineDataSet.setValueTextSize(dpFontSize / 1.2247f);
                        break;
                    case 2:
                        lineDataSet.setColors(Color.parseColor("#FF0000"));
                        lineDataSet.setValueTextColor(Color.parseColor("#FF0000"));
                        lineDataSet.setValueTextSize(dpFontSize);

                        break;
                }

                lineDataSet.setLineWidth(1.4f);

                i++;
            }

            LineData lineData = new LineData(Arrays.<ILineDataSet>asList(lineDataSets));
            lineData.setDrawValues(true);

            chart.setData(lineData);

//            float xRange = chart.getXRange();

//            chart.setVisibleYRange(0.0f, xRange, YAxis.AxisDependency.LEFT);
//            chart.setVisibleYRange(0.0f, xRange, YAxis.AxisDependency.RIGHT);

            chart.invalidate();

            List<Object> outputList = new ArrayList<>();

            outputList.add((long) Math.round(1000.0f / min));
            outputList.add((long) Math.round(1000.0f / mean));
            outputList.add((long) Math.round(1000.0f / max));
        }
    }

    public void loadChart(boolean setActive) {
        if (chart == null ||
                minChartEntries == null || meanChartEntries == null || maxChartEntries == null) {
            maxChartEntries = new ArrayList<>();
            meanChartEntries = new ArrayList<>();
            minChartEntries = new ArrayList<>();
            createFrameratesChart(setActive);
            if (setActive) {
                this._chartActive = true;
            }
        }

        this._chartLoaded = true;
    }

    private void obliterateChart() {
        for (int i = 0; i < root.getChildCount(); i++) {
            View possibleChart = root.getChildAt(i);

            if (possibleChart instanceof Chart) {
                root.removeView(possibleChart);
            }
        }

        try {
            root.removeView(chart);
        } catch (Exception e) {

        }

        chart = null;

        setChartActive(false);

        clearChart();

        minChartEntries = null;
        meanChartEntries = null;
        maxChartEntries = null;

        this._chartLoaded = false;
        this._chartActive = false;
    }

    boolean isChartLoaded() {
        return this._chartLoaded;
    }
    //endregion

    private void onDoubleBackPressed() {
        //region 2:3 double-press prevention facility
        long currentTimestamp = System.nanoTime();

        if (lastBackDoublePressedTimestamp == 0)
            lastBackDoublePressedTimestamp = currentTimestamp;
        else { // ignoring interleaving double press (three single-presses wrongly generates two double-presses
            if (currentTimestamp - lastBackDoublePressedTimestamp < msBackDoublePressTriggerTime * 1000_000L)
                return; // aborting double press
        }
        //endregion

        boolean chartCurrentlyActive = isChartActive();

        setChartActive(!chartCurrentlyActive);

        lastBackDoublePressedTimestamp = currentTimestamp;
    }

    private void onSingleBackPressed() {
//        if(isChartActive()) {
//            chartThread.interrupt();
//        }
    }

    //region inner classes
    private enum ActivityMessageType {
        MESSAGE_BEGIN_INITIALIZATION,
        MESSAGE_COMPLETE_LOADED,
        MESSAGE_UPDATE_FRAMERATE_TEXT,
        MESSAGE_UPDATE_TOTAL_FRAMERATE_TEXT,
        MESSAGE_CLEAR_CHART_ENTRIES,
        MESSAGE_UPDATE_CHART
    }
    //endregion
}
