package me.mizhoux.antenna.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class RegionSelectorView extends View {

	private final float CIRCLE_RADIUS = 25.0F;

	public RegionSelectorView(Context context) {
		super(context);
	}

	public RegionSelectorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RegionSelectorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	private Rect rect = new Rect();
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private boolean mSystemDrawing = true;

	private int W;
	private int H;

    private int startX;
    private int startY;

    private int action = 0;
    private int edge   = 0;  // see insideEdge for edge type
    private int corner = 0;  // see insideCorner for corner type

    private int IN_PADDING  = 50;
    private int OUT_PADDING = 100;

    private int MIN_SIZE = 2 * IN_PADDING;
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mSystemDrawing) {
            W = this.getWidth();
            H = this.getHeight();

            Log.e("RegionSelectorView", "W: " + W + ", H: " + H);

			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.GREEN);
			paint.setStrokeWidth(5);

			rect = new Rect(0, 0, W, H);

			int initRegionHeight = MIN_SIZE;
			int initRegionWidth  = MIN_SIZE * 2;

			rect.set((rect.width() - initRegionWidth) / 2,
					(rect.height() - initRegionHeight) / 2,
					(rect.right + initRegionWidth) / 2,
					(rect.bottom + initRegionHeight) /2 );
		}

		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(rect, paint);

		paint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(rect.left, rect.top, CIRCLE_RADIUS, paint);
		canvas.drawCircle(rect.right, rect.top, CIRCLE_RADIUS, paint);
		canvas.drawCircle(rect.left, rect.bottom, CIRCLE_RADIUS, paint);
		canvas.drawCircle(rect.right, rect.bottom, CIRCLE_RADIUS, paint);
	}

    public Rect getSelectedRegion() {
        return rect;
    }

    public Rect getScreenRect() {
	    return new Rect(0, 0, W, H);
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {

	    mSystemDrawing = false;

		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();

                if (inRectOrOutPadding(rect, x, y)) {
                    startX = x;
                    startY = y;

                    action = getAction(x, y);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                handleActionMove(x, y);
                break;

            case MotionEvent.ACTION_UP:
                action = 0;
                edge = 0;
                corner = 0;
                break;

            default:
                break;
		}

		invalidate();
		return true;
	}

	private int getAction (int x, int y) {
		if (insideEdge(rect, x, y) > 0) {
			return 2; // 边缘拉伸

		} else if (insideCorner(rect, x, y) > 0) {
			return 3; // 角拉伸
		}

		return 0;
	}
	
	private void handleActionMove(int currX, int currY) {
		switch (action) {
            case 2:
                if (edge == 0) {
                    edge = insideEdge(rect, currX, currY);
                }

                if (edge > 0) {
                    updateRectSizeByEdge(currX, currY);
                }

                break;

            case 3:
                if (corner == 0) {
                    corner = insideCorner(rect, currX, currY);
                }

                if (corner > 0) {
                    updateRectSizeByCorner(currX, currY);
                }
                break;

            default:
                break;
		}
	}
	
	private void checkRectBoundary() {
		int W = this.getWidth();
		int H = this.getHeight();
		
		if (rect.left < 0) {
			rect.left = 0;
		} else if (rect.left > (W - MIN_SIZE) / 2) {
			rect.left = (W - MIN_SIZE) / 2;
		}

		if (rect.top < 0) {
			rect.top = 0;
		} else if (rect.top > (H - MIN_SIZE) / 2) {
			rect.top = (H - MIN_SIZE) / 2;
		}
		
		if (rect.right > W) {
			rect.right = W;
		}
		
		if (rect.bottom > H) {
			rect.bottom = H;
		}
		
		if (rect.right - rect.left < MIN_SIZE) {
			rect.right = rect.left + MIN_SIZE;
		}
		
		if (rect.bottom - rect.top < MIN_SIZE) {
			rect.bottom = rect.top + MIN_SIZE;
		}
	}
	
	private void updateRectSizeByCorner(int currX, int currY) {

		int deltaX = currX - startX;
		int deltaY = currY - startY;
		
		switch (corner) {
            case 1:
                rect.left += deltaX;
                rect.top += deltaY;

                rect.right -= deltaX;
                rect.bottom -= deltaY;
                break;
            case 2:
                rect.right += deltaX;
                rect.top += deltaY;

                rect.left -= deltaX;
                rect.bottom -= deltaY;
                break;
            case 3:
                rect.left += deltaX;
                rect.bottom += deltaY;

                rect.right -= deltaX;
                rect.top -= deltaY;
                break;
            case 4:
                rect.right += deltaX;
                rect.bottom += deltaY;

                rect.left -= deltaX;
                rect.top -= deltaY;
                break;
		}
		
		checkRectBoundary();

		startX = currX;
		startY = currY;
	}
	
	private void updateRectSizeByEdge(int currX, int currY) {
		
		int deltaX = currX - startX;
		int deltaY = currY - startY;
		
		switch (edge) {
            case 1: // 左边缘
                rect.left += deltaX;
                rect.right -= deltaX;
                break;

            case 2: // 上边缘
                rect.top += deltaY;
                rect.bottom -= deltaY;
                break;

            case 3: // 右边缘
                rect.right += deltaX;
                rect.left -= deltaX;
                break;

            case 4: // 下边缘
                rect.bottom += deltaY;
                rect.top -= deltaY;
                break;
		}
		
		checkRectBoundary();

		startX = currX;
		startY = currY;
	}

	private int insideCorner(Rect r, int x, int y) {
		if (x >= (r.left - OUT_PADDING) && x < (r.left + IN_PADDING)) {
			if (y >= (r.top - OUT_PADDING) && y < (r.top + IN_PADDING)) {
				return 1; // 左上角
			}

			if (y <= (r.bottom + OUT_PADDING) && y > (r.bottom - IN_PADDING)) {
				return 3; // 左下角
			}
		}
		
		if (x > (r.right - IN_PADDING) && x <= (r.right + OUT_PADDING)) {
			if (y >= (r.top - OUT_PADDING) && y < (r.top + IN_PADDING)) {
				return 2; // 右上角
			}

			if (y <= (r.bottom + OUT_PADDING) && y > (r.bottom - IN_PADDING)) {
				return 4; // 右下角
			}
		}

		return 0;
	}
	
	/**
	 * 是否在框选矩形或者外边距内
	 */
	private boolean inRectOrOutPadding(Rect r, int x, int y) {
		return x >= (r.left - OUT_PADDING)
                && x <= (r.right + OUT_PADDING)
				&& y >= (r.top - OUT_PADDING)
                && y <= (r.bottom + OUT_PADDING);
	}

	private int insideEdge(Rect r, int x, int y) {

		if (y > (r.top + IN_PADDING) && y < (r.bottom - IN_PADDING)) {
			if (x >= (r.left - OUT_PADDING) && x < (r.left + IN_PADDING)) {
				return 1; // 左边缘
			}
			
			if (x > (r.right - IN_PADDING) && x <= (r.right + OUT_PADDING)) {
				return 3; // 右边缘
			}
		}

		if (x > (r.left + IN_PADDING) && x < (r.right - IN_PADDING)) {
			if (y >= (r.top - OUT_PADDING) && y < (r.top + IN_PADDING)) { // y - top < PADDING
				return 2; // 上边缘
			}

			if (y > (r.bottom - IN_PADDING) && y <= (r.bottom + OUT_PADDING) ) { // bottom - y < PADDING
				return 4; // 下边缘
			}
		}

		return 0;
	}

	@Override
	public boolean performClick() {
		// Calls the super implementation, which generates an AccessibilityEvent
		// and calls the onClick() listener on the view, if any
		super.performClick();

		// Handle the action for the custom click here

		return true;
	}


	public void change(){
		W=this.getWidth()+20;
		rect.left -=20;
		rect.right +=20;
		invalidate();
	}
	public void reset(){
		W=this.getWidth()-20;
		rect.left +=20;
		rect.right -=20;
		invalidate();
	}

}

