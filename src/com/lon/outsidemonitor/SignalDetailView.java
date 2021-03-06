package com.lon.outsidemonitor;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import com.lon.outsidemonitor.signal.SignalAmpl;
import com.lon.outsidemonitor.signal.SignalAmplPoint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class SignalDetailView extends View {

	static final int sampleRate = 8000;

	static final int LeftMargin = 40;
	static final int BottomMargin = 40;
	static final int GridSize = 80;

	public boolean stoped = false;

	int centerIndex = sampleRate / 2; // 光标对应的数据的索引
	int xCursorIndex = 300; // X光标的位置

	float PixelPerData = 2; // 每个数据占用的像素数

	private float upper = 256; // 上限
	private float lower = 0;// 下限

	private float[] signalData;

	float[] pointsList = new float[16 * 1000];// 入点 ，最大点，最小点，出点

	Paint paint = new Paint();
	PathEffect effects = new DashPathEffect(new float[] { 5, 5, 5, 5 }, 1); // 虚线效果
	DecimalFormat dfXSCale = new DecimalFormat();
	DecimalFormat dfYSCale = new DecimalFormat();
	DecimalFormat dfText = new DecimalFormat();

	int screenWidth = 800;
	int screenHeight = 400;

	// 三角形
	Bitmap triangle; // 三角形的图片
	float yCenterVal = 0;

	DrawMode drawMode = DrawMode.SignalAmpl;

	/*
	 * 信号幅度的数值
	 */
	float upperAmpl = 1; // 显示的幅度的最大值
	float lowerAmpl = 0; // 显示的幅度的最小值
	float PixelPerSecond = 20; // 每秒钟占几个像素
	ArrayList<AmplPoint> amplPoints = new ArrayList<AmplPoint>();

	float xCursorSecond = 15; // 光标对应的时间

	// 日期处理函数
	Calendar calendar = Calendar.getInstance();
	SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm:ss");

	// 信号频谱绘制的相关参数
	int specCenterIndex = 150; // 光标对应的数据的索引
	int specXCursorIndex = 300; // X光标的位置
	float specPixelPerData = 5; // 每个数据占用的像素数
	private float[] specSignalData; // 信号的频谱数据
	private float specUpper = 8000; // 上限
	private float specLower = 0;// 下限
	float specYCenterVal = 4000;

	public SignalDetailView(Context context, AttributeSet attrs) {
		// TODO Auto-generated constructor stub
		super(context, attrs);

		dfXSCale.applyPattern("0.0000");
		dfYSCale.applyPattern("0.00");
		dfText.applyPattern("0.00000");

		BitmapDrawable bmpDraw = (BitmapDrawable) context.getResources()
				.getDrawable(R.drawable.triangle);
		triangle = bmpDraw.getBitmap();

		calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));

	}

	public void setDisplayMode(DrawMode mode) {
		if (mode != drawMode) {

			drawMode = mode;
			if (mode == DrawMode.SignalData) {
				PixelPerData = -1;
			} else if (mode == DrawMode.SignalAmpl) {
				amplPoints.clear();
			}
			invalidate(); // 刷新
		}
	}

	public void setLimit(float upper, float lower) {
		// this.upper=upper;
		// this.lower=lower;
		// this.lowerAmpl = 0;
		// this.upperAmpl = upper;
		//
		// if (drawMode == DrawMode.SignalData) // 实时数据
		// {
		//
		// this.postInvalidate();
		// } else if (drawMode == DrawMode.SignalAmpl) // 信号幅度
		// {
		//
		// this.postInvalidate(); // 刷新
		// }
	}

	public void zoomIn() {
		if (PixelPerData < 0)
			return;
		if (drawMode == DrawMode.SignalData) {
			if (PixelPerData < 20) {
				PixelPerData = PixelPerData * 1.2f;
			}
		} else if (drawMode == DrawMode.SignalAmpl) {
			if (PixelPerData * 2 > screenWidth - LeftMargin)
				return;
			PixelPerSecond = PixelPerSecond * 2;
			xCursorSecond = xCursorSecond * 2;

		} else if (drawMode == DrawMode.SignalFreq) {
			if (specPixelPerData < 20) {
				specPixelPerData = specPixelPerData * 1.2f;
			}
		}

		this.invalidate();
	}

	public void zoomOut() {
		if (PixelPerData < 0)
			return;
		if (drawMode == DrawMode.SignalData) {
			if (PixelPerData > 0.02) {
				PixelPerData = PixelPerData / 1.2f;
			}
		} else if (drawMode == DrawMode.SignalAmpl) {

			if (PixelPerSecond / 2 < 1)
				return;
			PixelPerSecond = PixelPerSecond / 2;
			xCursorSecond = xCursorSecond / 2;
		} else if (drawMode == DrawMode.SignalFreq) {
			if (specPixelPerData > 0.02) {
				specPixelPerData = specPixelPerData / 1.2f;
			}
		}
		this.invalidate();
	}

	public void zoomYIn() {
		if (drawMode == DrawMode.SignalData) {
			float t1 = yCenterVal + (upper - yCenterVal) / 1.2f;
			float t2 = yCenterVal - (yCenterVal - lower) / 1.2f;

			upper = t1;
			lower = t2;
		} else if (drawMode == DrawMode.SignalAmpl) {
			upperAmpl = upperAmpl * 1.2f;

		}

		this.invalidate();
	}

	public void zoomYOut() {
		if (drawMode == DrawMode.SignalData) {
			float t1 = yCenterVal + (upper - yCenterVal) * 1.2f;
			float t2 = yCenterVal - (yCenterVal - lower) * 1.2f;

			upper = t1;
			lower = t2;
		} else if (drawMode == DrawMode.SignalAmpl) {
			upperAmpl = upperAmpl / 1.2f;
		}

		this.invalidate();
	}

	/*
	 * 
	 */
	public void moveYCursor(int offset) {
		if (drawMode == DrawMode.SignalData) {
			float triangleY = (screenHeight - BottomMargin)
					- (yCenterVal - lower) * (screenHeight - BottomMargin)
					/ (upper - lower);

			if (triangleY - offset > screenHeight - BottomMargin)
				return;
			if (triangleY - offset < 0)
				return;

			yCenterVal = lower
					+ (screenHeight - LeftMargin - triangleY + offset)
					* (upper - lower) / (screenHeight - BottomMargin);
		} else if (drawMode == DrawMode.SignalFreq) {
			float triangleY = (screenHeight - BottomMargin)
					- (specYCenterVal - specLower)
					* (screenHeight - BottomMargin) / (specUpper - specLower);

			if (triangleY - offset > screenHeight - BottomMargin)
				return;
			if (triangleY - offset < 0)
				return;

			specYCenterVal = specLower
					+ (screenHeight - LeftMargin - triangleY + offset)
					* (specUpper - specLower) / (screenHeight - BottomMargin);
		}

		postInvalidate(); // 刷新
	}

	public void refreshRawData(float[] data) {
		if (stoped)
			return;
		if ((data == null) || (data.length <= 0))
			return;
		if ((this.signalData == null) || (this.signalData.length < data.length)) {
			signalData = new float[data.length];

		}

		float maxAD = 0;
		float minAD = 4096;

		for (int i = 0; i < data.length; i++) {
			signalData[i] = data[i];
			if (data[i] > maxAD) {
				maxAD = data[i];
			}
			if (data[i] < minAD) {
				minAD = data[i];
			}
		}
		this.lower = minAD - (maxAD - minAD) * 0.1f;
		this.upper = maxAD + (maxAD - minAD) * 0.1f;
		if (drawMode == DrawMode.SignalData) {
			postInvalidate(); // 刷新
		}
	}

	public void refreshSpectrumData(float[] data) {
		if (stoped)
			return;
		this.specSignalData = data;
		if (drawMode == DrawMode.SignalFreq) {
			postInvalidate(); // 刷新
		}
	}

	/*
	 * 移动光标
	 */
	public void moveXCursor(int offset) {

		if (PixelPerData < 0)
			return;
		if (drawMode == DrawMode.SignalData) {
			if (PixelPerData > 1) {
				if (offset > 0) {
					if (xCursorIndex + PixelPerData > screenWidth - LeftMargin)
						return;
					xCursorIndex = (int) (PixelPerData + xCursorIndex + 0.5f);
					centerIndex++;
				} else {
					if (xCursorIndex - PixelPerData < 0)
						return;
					xCursorIndex = (int) (xCursorIndex - PixelPerData + 0.5f);
					centerIndex--;
				}
			} else {
				if (xCursorIndex + offset > screenWidth - LeftMargin)
					return;
				if (xCursorIndex + offset < 0)
					return;
				xCursorIndex += offset;

				centerIndex += (int) (offset / PixelPerData);
			}
		} else if (drawMode == DrawMode.SignalAmpl) {
			if (PixelPerSecond > 1) {
				if (offset > 0) // 往右移动
				{
					if (xCursorSecond - 1 < 0)
						return;
					xCursorSecond -= 1;
				} else {
					if (xCursorSecond + 1 > (screenWidth - LeftMargin)
							/ PixelPerSecond)
						return;
					xCursorSecond += 1;
				}
			}
		} else if (drawMode == DrawMode.SignalFreq) {
			if (specPixelPerData > 1) {
				if (offset > 0) {
					if (specXCursorIndex + specPixelPerData > screenWidth
							- LeftMargin)
						return;
					specXCursorIndex = (int) (specPixelPerData
							+ specXCursorIndex + 0.5f);
					
				} else {
					if (specXCursorIndex - specPixelPerData < 0)
						return;
					specXCursorIndex = (int) (specXCursorIndex
							- specPixelPerData + 0.5f);
					
				}
			} else {
				if (specXCursorIndex + offset > screenWidth - LeftMargin)
					return;
				if (specXCursorIndex + offset < 0)
					return;
				specXCursorIndex += offset;

				
			}
		}

		postInvalidate(); // 刷新
	}

	public void addSignalAmpl(SignalAmpl ampl) {
		if (stoped)
			return;

		int cnt = ampl.getCount();

		for (int i = 0; i < cnt; i++) {
			SignalAmplPoint sPoint = ampl.getAmplPoint(i);
			float sAmpl = sPoint.getAmpl();
			if (sAmpl * 1.1f > upperAmpl) {
				upperAmpl = sAmpl * 1.1f;
			}
			AmplPoint point = new AmplPoint(sAmpl, sPoint.getTime());

			if (amplPoints.size() > 500) {
				amplPoints.remove(0);
			}
			amplPoints.add(point);
		}
		if (drawMode == DrawMode.SignalAmpl) {
			postInvalidate(); // 刷新
		}

	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);

		if (drawMode == DrawMode.SignalData) {
			drawSignalData(canvas);
		} else if (drawMode == DrawMode.SignalAmpl) {
			drawSignalAmpl(canvas);
		} else if (drawMode == DrawMode.SignalFreq) {
			drawSignalFreq(canvas);
		}

	}

	private void drawLines(Canvas canvas, float[] pointsList, int pointNum) {
		if (pointNum >= 2) {
			if ((pointNum % 2) == 0) {
				canvas.drawLines(pointsList, 0, pointNum * 2, paint);

				if (pointNum >= 4) {
					canvas.drawLines(pointsList, 2, (pointNum - 2) * 2, paint);
				}
			} else {
				canvas.drawLines(pointsList, 0, (pointNum - 1) * 2, paint);
				canvas.drawLines(pointsList, 2, (pointNum - 1) * 2, paint);
			}
		}
	}

	/*
	 * 绘制信号原始数据
	 */
	private void drawSignalData(Canvas canvas) {

		canvas.drawColor(Color.rgb(0, 0, 0)); // 设置背景为黑色
		if (signalData == null)
			return;
		if (signalData.length <= 1)
			return;
		screenWidth = this.getWidth();
		screenHeight = this.getHeight();
		int displayWidth = screenWidth - LeftMargin;

		if (PixelPerData < 0) {
			PixelPerData = displayWidth * 1f / sampleRate;
			xCursorIndex = displayWidth / 2;
		}
		// 绘制垂直网格
		int startX = LeftMargin;
		float startTime = (centerIndex - xCursorIndex * 1f / PixelPerData)
				/ sampleRate;
		float timeInterval = GridSize * 1f / PixelPerData / sampleRate;
		paint.setColor(Color.GRAY);
		int lineIndex = 0;
		while (startX < screenWidth) {
			if (lineIndex >= 1) {
				paint.setPathEffect(effects);
			}
			canvas.drawLine(startX, 0, startX, screenHeight - BottomMargin,
					paint);

			paint.setPathEffect(null);
			canvas.drawText(dfXSCale.format(startTime), startX, screenHeight
					- BottomMargin + 12, paint);
			startTime += timeInterval;
			startX += GridSize;
			lineIndex++;
		}

		// 绘制水平网格
		int startY = screenHeight - BottomMargin;

		paint.setColor(Color.GRAY);
		lineIndex = 0;
		while (startY > 0) {
			if (lineIndex == 1) {
				paint.setPathEffect(effects);
			}

			canvas.drawLine(0 + LeftMargin, startY, screenWidth, startY, paint);
			startY -= GridSize;
			lineIndex++;
		}
		paint.setPathEffect(null);
		paint.setColor(Color.rgb(0, 255, 0));

		synchronized (signalData) {
			if (signalData != null) {
				// 左边最远的一个数据的索引
				int leftIndex = (int) (centerIndex - xCursorIndex
						/ PixelPerData - 1);

				if (leftIndex < 0) {
					leftIndex = 0;
				}
				int length = signalData.length;
				int prevNum = 0;

				float x1 = 0;
				float y1 = 0;
				float yMax = 0;
				float yMin = 0;
				float x2 = 0;
				float y2 = 0;
				for (int i = leftIndex; i < length; i++) {
					float x = xCursorIndex + (i - centerIndex) * PixelPerData;
					x = x + LeftMargin;
					float y = screenHeight - BottomMargin
							- (signalData[i] - lower)
							* (screenHeight - BottomMargin) / (upper - lower);
					if (prevNum <= 0) {
						x1 = x;
						y1 = y;
						yMax = y;
						yMin = y;
						x2 = x;
						y2 = y;
						prevNum = 1;
					} else {
						if (Math.abs(x - x1) < 1) // 在同一条垂直线上
						{
							if (y > yMax) {
								yMax = y;
							}
							if (y < yMin) {
								yMin = y;
							}
							x2 = x;
							y2 = y;
							prevNum++;
						} else {
							// 绘制曲线
							canvas.drawLine(x1, yMin, x2 + 0.5f, yMax, paint);
							canvas.drawLine(x, y, x2, y2, paint);
							//
							x1 = x;
							y1 = y;
							yMax = y;
							yMin = y;
							x2 = x;
							y2 = y;
							prevNum = 1;
						}
					}

				}

			}
		}

		// 抹掉左边的漏条纹
		paint.setColor(Color.BLACK);
		canvas.drawRect(0, 0, LeftMargin, screenHeight - BottomMargin, paint);

		// 绘制Y刻度
		startY = screenHeight - BottomMargin;

		paint.setColor(Color.GRAY);
		lineIndex = 0;
		float startValue = lower;

		float valueScale = (upper - lower) * GridSize
				/ (screenHeight - BottomMargin);
		while (startY > 0) {
			canvas.drawText(dfYSCale.format(startValue), 0, startY, paint);
			startValue += valueScale;
			startY -= GridSize;
			lineIndex++;
		}

		// 绘制光标

		paint.setColor(Color.rgb(65, 105, 225));
		// X光标
		canvas.drawLine(xCursorIndex + LeftMargin, 0,
				xCursorIndex + LeftMargin, screenHeight - BottomMargin, paint);
		paint.setColor(Color.RED);
		float markX = xCursorIndex - 25 + LeftMargin;
		float markY = screenHeight - BottomMargin;
		canvas.drawRect(markX, markY, markX + 50, markY + 15, paint);
		paint.setColor(Color.rgb(0, 255, 0));
		float cursorTime = centerIndex * 1f / sampleRate;
		canvas.drawText(dfText.format(cursorTime), markX + 3, screenHeight
				- BottomMargin + 12, paint);
		paint.setColor(Color.rgb(65, 105, 225));
		// Y光标
		// if (signalData != null && centerIndex >= 0
		// && centerIndex < signalData.length) {
		// float cursorVal = screenHeight - BottomMargin
		// - (signalData[centerIndex] - lower)
		// * (screenHeight - BottomMargin) / (upper - lower);//
		//
		// canvas.drawLine(LeftMargin, cursorVal, screenWidth, cursorVal,
		// paint);
		//
		// }
		// 绘制Ymark
		float triangleY = (screenHeight - BottomMargin) - (yCenterVal - lower)
				* (screenHeight - BottomMargin) / (upper - lower);

		canvas.drawBitmap(triangle, LeftMargin - 15, triangleY - 8, paint);
	}

	/**
	 * 绘制信号幅度
	 * 
	 */
	private void drawSignalAmpl(Canvas canvas) {
		canvas.drawColor(Color.rgb(0, 0, 0)); // 设置背景为黑色
		screenWidth = this.getWidth();
		screenHeight = this.getHeight();
		int displayWidth = screenWidth - LeftMargin;

		if (pointsList == null || pointsList.length / 8 < displayWidth) {
			pointsList = new float[displayWidth * 8 + 500];
		}

		// 绘制垂直网格
		int startX = screenWidth;
		long startTime = -1;
		if (amplPoints.size() > 0) {
			startTime = amplPoints.get(amplPoints.size() - 1).millisTime;
		}
		float timeInterval = GridSize * 1f / PixelPerSecond;
		paint.setColor(Color.GRAY);

		paint.setPathEffect(effects);
		while (true) {

			canvas.drawLine(startX, 0, startX, screenHeight - BottomMargin,
					paint);

			startX -= GridSize;
			if (startX <= LeftMargin) {
				paint.setPathEffect(null);
				canvas.drawLine(LeftMargin, 0, LeftMargin, screenHeight
						- BottomMargin, paint);
				break;
			}
		}
		// 显示X刻度
		if (startTime > 0) {
			startX = screenWidth;
			while (true) {
				calendar.setTimeInMillis(startTime);
				canvas.drawText(dfTime.format(calendar.getTime()), startX - 20,
						screenHeight - BottomMargin + 12, paint);
				startX -= GridSize;
				startTime -= (long) (timeInterval * 1000);
				if (startX <= LeftMargin) {
					break;
				}
			}
			calendar.setTimeInMillis((long) startTime);

		}

		// 绘制Y网格
		int startY = screenHeight - BottomMargin;

		paint.setColor(Color.GRAY);
		int lineIndex = 0;
		while (startY > 0) {
			if (lineIndex == 1) {
				paint.setPathEffect(effects);
			}

			canvas.drawLine(0 + LeftMargin, startY, screenWidth, startY, paint);
			startY -= GridSize;
			lineIndex++;
		}
		// 绘制数据曲线
		paint.setPathEffect(null);
		paint.setColor(Color.rgb(0, 255, 0));

		int matchIndex = -1;
		float matchX = 0;
		float matchAmpl = 0;
		float matchTime = 0;
		float matchY = 0;
		synchronized (amplPoints) {
			int ptSize = amplPoints.size();
			if (ptSize >= 2) {
				AmplPoint pointBase = amplPoints.get(ptSize - 1);
				int prevNum = 0;

				float x1 = 0;
				float y1 = 0;
				float yMax = 0;
				float yMin = 0;
				float x2 = 0;
				float y2 = 0;

				for (int i = ptSize - 1; i >= 0; i--) {
					AmplPoint point = amplPoints.get(i);
					float x = screenWidth
							- (pointBase.millisTime - point.millisTime)
							* 0.001f * PixelPerSecond;
					float y = (screenHeight - BottomMargin)
							- (point.ampl - lowerAmpl)
							* (screenHeight - BottomMargin)
							/ (upperAmpl - lowerAmpl);
					if (x < LeftMargin) {
						break;
					}
					if (prevNum <= 0) {
						x1 = x;
						y1 = y;
						yMax = y;
						yMin = y;
						x2 = x;
						y2 = y;
						prevNum = 1;
					} else {
						if (Math.abs(x - x1) < 1) // 在同一条垂直线上
						{
							if (y > yMax) {
								yMax = y;
							}
							if (y < yMin) {
								yMin = y;
							}
							x2 = x;
							y2 = y;
							prevNum++;
						} else {

							// 绘制曲线
							canvas.drawLine(x1, yMin, x2, yMax, paint);
							canvas.drawLine(x, y, x2, y2, paint);
							//
							x1 = x;
							y1 = y;
							yMax = y;
							yMin = y;
							x2 = x;
							y2 = y;
							prevNum = 1;
						}
					}
				}
			}
			if (ptSize > 0) {
				// if (PixelPerSecond > 20) {
				// for (int i = 0; i < pointNum; i++) {
				//
				// canvas.drawRect(pointsList[i * 2] - 2,
				// pointsList[i * 2 + 1] - 2, pointsList[i * 2] + 2,
				// pointsList[i * 2 + 1] + 2, paint);
				//
				// }
				// }
			}

		}

		// 绘制垂直光标
		paint.setColor(Color.rgb(65, 105, 225));
		if (matchIndex > 0) {

			// X光标
			canvas.drawLine(matchX, 0, matchX, screenHeight - BottomMargin,
					paint);
			// 绘制日期
			calendar.setTimeInMillis((long) (matchTime * 1000));
			paint.setColor(Color.RED);
			if (matchX < LeftMargin + (screenWidth - LeftMargin) / 2) // 靠左边
			{
				if (matchY > (screenHeight - BottomMargin) / 2) {

					canvas.drawText(dfXSCale.format(matchAmpl), matchX + 5,
							matchY - 30, paint);
					canvas.drawText(dfTime.format(calendar.getTime()),
							matchX + 5, matchY - 15, paint);
				} else {
					canvas.drawText(dfXSCale.format(matchAmpl), matchX + 5,
							matchY + 15, paint);
					canvas.drawText(dfTime.format(calendar.getTime()),
							matchX + 5, matchY + 30, paint);
				}
			} else {
				if (matchY > (screenHeight - BottomMargin) / 2) {

					canvas.drawText(dfXSCale.format(matchAmpl), matchX - 50,
							matchY - 30, paint);
					canvas.drawText(dfTime.format(calendar.getTime()),
							matchX - 50, matchY - 15, paint);
				} else {
					canvas.drawText(dfXSCale.format(matchAmpl), matchX - 50,
							matchY + 15, paint);
					canvas.drawText(dfTime.format(calendar.getTime()),
							matchX - 50, matchY + 30, paint);
				}
			}

		} else {
			float cursorX = screenWidth - xCursorSecond * PixelPerSecond;

			if (cursorX > LeftMargin) {
				// X光标
				canvas.drawLine(cursorX, 0, cursorX, screenHeight
						- BottomMargin, paint);
			}
		}

		// 抹掉左边的漏条纹
		paint.setColor(Color.BLACK);
		canvas.drawRect(0, 0, LeftMargin, screenHeight - BottomMargin, paint);

		// 绘制Y刻度
		startY = screenHeight - BottomMargin;

		paint.setColor(Color.GRAY);
		lineIndex = 0;
		float startValue = lowerAmpl;
		float valueScale = (upperAmpl - lowerAmpl) * GridSize
				/ (screenHeight - BottomMargin);
		while (startY > 0) {
			canvas.drawText(dfYSCale.format(startValue), 0, startY, paint);
			startValue += valueScale;
			startY -= GridSize;
			lineIndex++;
		}

	}

	public void updateSpecCenter(int center) {
		specCenterIndex = center;
	}

	/*
	 * 绘制信号的频谱
	 */
	private void drawSignalFreq(Canvas canvas) {

		canvas.drawColor(Color.rgb(0, 0, 0)); // 设置背景为黑色
		screenWidth = this.getWidth();
		screenHeight = this.getHeight();
		int displayWidth = screenWidth - LeftMargin;

//		if (pointsList == null || pointsList.length / 8 < displayWidth) {
//			pointsList = new float[displayWidth * 8];
//		}

		if (specSignalData != null) {
			float maxSpec = 0;
			int maxIndex = 0;
			for (int i = 0; i < specSignalData.length; i++) {
				if (specSignalData[i] > maxSpec) {
					maxSpec = specSignalData[i];
					maxIndex = i;
				}
			}
			if (stoped == false) {
				// specCenterIndex = maxIndex;
				specXCursorIndex = screenWidth / 2;
				//specPixelPerData = 2;
			}
			specUpper = maxSpec * (1 + 0.05f);
			specYCenterVal = specUpper / 2;
		}

		// 绘制垂直网格
		int startX = LeftMargin;
		float startTime = (specCenterIndex - specXCursorIndex * 1f
				/ specPixelPerData);

		float timeInterval = GridSize * 1f / specPixelPerData;
		paint.setColor(Color.GRAY);
		int lineIndex = 0;
		while (startX < screenWidth) {
			if (lineIndex >= 1) {
				paint.setPathEffect(effects);
			}
			canvas.drawLine(startX, 0, startX, screenHeight - BottomMargin,
					paint);

			paint.setPathEffect(null);
			canvas.drawText(dfYSCale.format(startTime), startX, screenHeight
					- BottomMargin + 12, paint);
			startTime += timeInterval;
			startX += GridSize;
			lineIndex++;
		}

		// 绘制水平网格
		int startY = screenHeight - BottomMargin;

		paint.setColor(Color.GRAY);
		lineIndex = 0;
		while (startY > 0) {
			if (lineIndex == 1) {
				paint.setPathEffect(effects);
			}

			canvas.drawLine(0 + LeftMargin, startY, screenWidth, startY, paint);
			startY -= GridSize;
			lineIndex++;
		}
		paint.setPathEffect(null);
		paint.setColor(Color.rgb(0, 255, 0));

		// 绘制曲线
		if (specSignalData != null) {

			if (specPixelPerData >= 1) {
				// 左边最远的一个数据的索引
				int leftIndex = (int) (specCenterIndex - specXCursorIndex
						/ specPixelPerData);
				if (leftIndex < 0) {
					leftIndex = 0;
				}
				int pointNum = 0;
				for (int i = leftIndex; i < specSignalData.length; i++) {
					float x = specXCursorIndex + (i - specCenterIndex)
							* specPixelPerData;
					if (x > displayWidth)
						break;
					pointsList[pointNum * 2] = x + LeftMargin;

					pointsList[pointNum * 2 + 1] = screenHeight - BottomMargin
							- (specSignalData[i] - specLower)
							* (screenHeight - BottomMargin)
							/ (specUpper - specLower);//

					pointNum++;
				}
				// 绘制曲线
				drawLines(canvas, pointsList, pointNum);
				if (specPixelPerData > 15) {
					for (int i = 0; i < pointNum; i++) {

						canvas.drawRect(pointsList[i * 2] - 4,
								pointsList[i * 2 + 1] - 4,
								pointsList[i * 2] + 4,
								pointsList[i * 2 + 1] + 4, paint);

					}
				}

			} else {

				int freqStartIndex = (int) (specCenterIndex - specXCursorIndex
						/ specPixelPerData - 0.5f);

				if (freqStartIndex < 0) {
					freqStartIndex = 0;
				}
				int pointNum = 0;
				int dataLen = specSignalData.length;
				float maxY = 0;
				float minY = 0;
				float firstY=0;
				float lastY = 0;
				int lastX = Integer.MIN_VALUE;
				boolean newPt=false;
				for (int i = freqStartIndex; i < dataLen; i++) {
					int posX = (int) (specXCursorIndex - (specCenterIndex - i)
							* specPixelPerData + LeftMargin);
					if(posX>screenWidth) break;
					float posY=screenHeight - BottomMargin
							- (specSignalData[i] - specLower)
							* (screenHeight - BottomMargin)
							/ (specUpper - specLower);
					if(posX!=lastX)
					{
						if(lastX!=Integer.MIN_VALUE) //数据点
						{
							pointsList[pointNum * 8+0]=lastX;
							pointsList[pointNum * 8+1]=firstY;
							pointsList[pointNum * 8+2]=lastX;
							pointsList[pointNum * 8+3]=minY;
							pointsList[pointNum * 8+4]=lastX;
							pointsList[pointNum * 8+5]=maxY;
							pointsList[pointNum * 8+6]=lastX;
							pointsList[pointNum * 8+7]=lastY;
							
							pointNum++;
							newPt=false;
							if ((pointNum + 1) * 8 >= dataLen)
								break;
						}
						firstY=posY;
						minY=posY;
						maxY=posY;
						lastX=posX;
						lastY=posY;
						newPt=true;
					}
					else
					{
						if(posY>maxY)
						{
							maxY=posY;
						}
						if(posY<minY)
						{
							minY=posY;
						}
						lastY=posY;
					}
					
				}
				
				drawLines(canvas, pointsList, pointNum * 4);
			}
		}
		// 抹掉左边的漏条纹
		paint.setColor(Color.BLACK);
		canvas.drawRect(0, 0, LeftMargin, screenHeight - BottomMargin, paint);

		// 绘制Y刻度
		startY = screenHeight - BottomMargin;

		paint.setColor(Color.GRAY);
		lineIndex = 0;
		float startValue = specLower;
		float valueScale = (specUpper - specLower) * GridSize
				/ (screenHeight - BottomMargin);
		while (startY > 0) {
			canvas.drawText(dfYSCale.format(startValue), 0, startY, paint);
			startValue += valueScale;
			startY -= GridSize;
			lineIndex++;
		}

		// 绘制光标

		paint.setColor(Color.rgb(65, 105, 225));
		// X光标
		canvas.drawLine(specXCursorIndex + LeftMargin, 0, specXCursorIndex
				+ LeftMargin, screenHeight - BottomMargin, paint);
		paint.setColor(Color.RED);
		float markX = specXCursorIndex - 25 + LeftMargin;
		float markY = screenHeight - BottomMargin;
		canvas.drawRect(markX, markY, markX + 50, markY + 15, paint);
		paint.setColor(Color.rgb(0, 255, 0));
		float cursorTime = specCenterIndex * 1f;
		canvas.drawText(dfYSCale.format(cursorTime), markX + 3, screenHeight
				- BottomMargin + 12, paint);
		paint.setColor(Color.rgb(65, 105, 225));
		// Y光标
		if (specSignalData != null && specCenterIndex >= 0
				&& specCenterIndex < specSignalData.length) {
			float cursorVal = screenHeight - BottomMargin
					- (specSignalData[specCenterIndex] - specLower)
					* (screenHeight - BottomMargin) / (specUpper - specLower);//

			canvas.drawLine(LeftMargin, cursorVal, screenWidth, cursorVal,
					paint);

		}
		// 绘制Ymark
		float triangleY = (screenHeight - BottomMargin)
				- (specYCenterVal - specLower) * (screenHeight - BottomMargin)
				/ (specUpper - specLower);

		canvas.drawBitmap(triangle, LeftMargin - 15, triangleY - 8, paint);
	}

	class AmplPoint {
		float ampl; // 幅度
		long millisTime; // 日期

		public AmplPoint(float ampl, long millisTime) {
			// TODO Auto-generated constructor stub
			this.ampl = ampl;
			this.millisTime = millisTime;
		}
	}
}
