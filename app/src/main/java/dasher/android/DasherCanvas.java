package dasher.android;

import dasher.CCustomColours;
import dasher.CDasherInput;
import dasher.CDasherScreen;
import dasher.CDasherView.MutablePoint;
import dasher.CInputFilter;
import dasher.CDasherView.Point;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;

public class DasherCanvas extends SurfaceView implements Callback, CDasherScreen {
	private final ADasherInterface intf;
	private final SurfaceHolder holder;
    private boolean bReady;

	/** coordinates of last touch */
	private int x,y;
    
	public DasherCanvas(Context context, ADasherInterface intf) {
		super(context);
		if (intf==null) throw new NullPointerException();//just do it now!
		this.intf=intf;
		holder = getHolder();
		holder.addCallback(this);
	}

	protected void onMeasure(int widthMS, int heightMS) {
		//Log.d("DasherIME","onMeasure ("+MeasureSpec.toString(widthMS)+","+MeasureSpec.toString(heightMS)+")");
		final int aspectPercent = (int)PreferenceManager.getDefaultSharedPreferences(getContext()).getLong("DisplayHeight", 100);
		//compute desired width, such that height can be aspectPercent of that, and satisfy constraints.
		int w;
		switch (MeasureSpec.getMode(widthMS)) {
		case MeasureSpec.EXACTLY:
			w = MeasureSpec.getSize(widthMS);
			switch (MeasureSpec.getMode(heightMS)) {
			case MeasureSpec.AT_MOST:
				if (MeasureSpec.getSize(heightMS) >= (w*aspectPercent)/100)
					break; //ok, as normal, just use aspect ratio
				//else fall through: even max height is not enough 
			case MeasureSpec.EXACTLY:
				//we'll have to use the height provided in the MeasureSpec,
				// and ignore the aspect ratio: we can't follow it!
				setMeasuredDimension(w,MeasureSpec.getSize(heightMS));
				return;
			}
			break;
		case MeasureSpec.AT_MOST: //width
			w = MeasureSpec.getSize(widthMS);
			if (MeasureSpec.getMode(heightMS)!=MeasureSpec.UNSPECIFIED) {
				int targetWidth = (MeasureSpec.getSize(heightMS)*100 + aspectPercent-1)/aspectPercent;
				if (targetWidth>w) {
					//to get desired aspect ratio, want canvas wider than allowed
					if (MeasureSpec.getMode(heightMS)==MeasureSpec.EXACTLY) {
						//and can't shrink height! So cannot obtain desired ratio.
						setMeasuredDimension(widthMS, MeasureSpec.getSize(heightMS));
						return;
					}
					//else, leave w at it's legal maximum; we'll shrink height
				} else
					w=targetWidth; //shrink width: computed height will be as spec'd.
			}
			break;
		default: //case MeasureSpec.UNSPECIFIED for width
			switch (MeasureSpec.getMode(heightMS)) {
			case MeasureSpec.EXACTLY: case MeasureSpec.AT_MOST: 
				//compute width that'll produce exactly the right height
				w=(MeasureSpec.getSize(heightMS)*100 + aspectPercent-1) / aspectPercent;
				break;
			default:{
					WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
					DisplayMetrics dm = new DisplayMetrics();
					wm.getDefaultDisplay().getMetrics(dm);
					w=Math.min((dm.heightPixels*100 + aspectPercent-1)/aspectPercent,dm.widthPixels);
				}
			}
		}
		setMeasuredDimension(w,(w*aspectPercent)/100);
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d("DasherIME",this+" surfaceChanged ("+width+", "+height+")");
		synchronized(DasherCanvas.this) {
			if (bReady) return;
			bReady = true;
		}
		intf.enqueue(new Runnable() {
			public void run() {
				intf.ChangeScreen(DasherCanvas.this);
			}
		});
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("DasherIME",this+" surfaceDestroyed");
		//this stops anything from being rendered to the surface ASAP:
		synchronized (this) {
			if (!bReady) return;
			bReady=false;
		}
		//then we employ a slower-acting switch to prevent any more
		//attempts to render frames (i.e. allowing any concurrent rendering
		// on the Dasher thread to finish) until we have another surfaceChanged:
		intf.enqueue(new Runnable() {
			public void run() {
				intf.ChangeScreen(null);
			}
		});
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		x=(int)e.getX(); y=(int)e.getY();
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			intf.KeyDown(System.currentTimeMillis(), 100);
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_OUTSIDE:
		case MotionEvent.ACTION_CANCEL:
			intf.KeyUp(System.currentTimeMillis(), 100);
			x=y=-1;
			break;
		}
		synchronized(this) {
			try {this.wait(1000);}
			catch (InterruptedException ex) {}
		}
		return true;
	}
	
	/* Gets (screen/pixel) x,y coordinates of last touch event*/
	public boolean GetCoordinates(MutablePoint coordinates) {
		if (x==-1) return false;
		coordinates.init(x,y);
		return true;
	}
	
	private Canvas canvas;
	private CCustomColours colours;
	
	/** Single Paint we'll use for everything - i.e. by changing
	 * all its parameters for each primitive.
	 * TODO: think about having multiple Paint objects caching different
	 * sets of parameters... 
	 */
	private final Paint p = new Paint();
	/** Use a single Rect object for every rectangle too, avoiding allocation...*/
	private final Rect r = new Rect();
	
	
	public void renderFrame() {
		synchronized(this) {
			if (!bReady) {
				return;
			}
		}
		canvas = holder.lockCanvas();
		//after a surfaceDestroyed(), renderFrame() can be called once more before we setCanvas(null) to stop it...
		// in which case, canvas==null and we won't be able to draw anything. But let's at least not NullPtrEx!
		if (canvas==null) return;
		try { 
			intf.NewFrame(System.currentTimeMillis());
		} finally {
			holder.unlockCanvasAndPost(canvas);
			canvas=null;
		}
		//tell the UI thread we're now ready for another touch event....
		synchronized(this) {this.notify();}
	}
	
	public void DrawCircle(int iCX, int iCY, int iR, int iFillColour, int iLineColour, int iLineWidth) {
		if (iFillColour!=-1) {
			p.setARGB(255, colours.GetRed(iFillColour), colours.GetGreen(iFillColour), colours.GetBlue(iFillColour));
			p.setStyle(Style.FILL);
			canvas.drawCircle(iCX, iCY, iR, p);
		}
		//and outline
		if (iLineWidth>0) {
			if (iLineColour==-1) iLineColour=3; //TODO hardcoded default
			p.setARGB(255, colours.GetRed(iLineColour), colours.GetGreen(iLineColour), colours.GetBlue(iLineColour));
			p.setStyle(Style.STROKE);
			p.setStrokeWidth(iLineWidth);
			canvas.drawCircle(iCX, iCY, iR, p);
		}
	}
	public void DrawRectangle(int x1, int y1, int x2, int y2,
			int iFillColour, int iOutlineColour,
			int iThickness) {
		r.left = x1; r.right = x2;
		r.top = y1; r.bottom = y2;
		if (iFillColour != -1) {
			p.setARGB(255, colours.GetRed(iFillColour), colours.GetGreen(iFillColour), colours.GetBlue(iFillColour));
			p.setStyle(Style.FILL);
			canvas.drawRect(r, p);
		}
		if (iThickness>0) {
			if (iOutlineColour==-1) iOutlineColour = 3; //TODO hardcoded default
			p.setARGB(255, colours.GetRed(iOutlineColour), colours.GetGreen(iOutlineColour), colours.GetBlue(iOutlineColour));
			p.setStyle(Style.STROKE);
			p.setStrokeWidth(iThickness); 
			canvas.drawRect(r,p);
		}
	}
	public void DrawString(String string, int x1, int y1, int Size) {
		p.setTextSize(Size);
		p.setARGB(255, 0, 0, 0);
		p.setStyle(Style.FILL_AND_STROKE);
		p.setStrokeWidth(1);
		p.getTextBounds(string, 0, string.length(), r);
		y1-=r.top;
		x1-=r.left;
		canvas.drawText(string, x1, y1, p);
	}
	
	public int GetHeight() { 
		return DasherCanvas.this.getHeight();
	}
	public int GetWidth() {
		return DasherCanvas.this.getWidth();
	}
	public void Polygon(Point[] Points, int fillColour, int iOutlineColour,
			int iWidth) {
		// TODO Auto-generated method stub
		
	}
	public void drawLine(int x0, int y0, int x1, int y1, int iWidth, int iColour) {
		p.setStrokeWidth(iWidth);
		p.setARGB(255, colours.GetRed(iColour), colours.GetGreen(iColour), colours.GetBlue(iColour));
		canvas.drawLine(x0, y0, x1, y1, p);
	}
	public void SetColourScheme(CCustomColours colours) {
		this.colours = colours;
	}
	public Point TextSize(String string, int iSize) {
		p.setTextSize(iSize);
		p.getTextBounds(string, 0, string.length(), r);
		return new Point(r.right-r.left, r.bottom-r.top);// - r.left, r.bottom - r.top);
	}
	
}
