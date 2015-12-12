package me.valour.bowls.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import me.valour.bowls.services.Kitchen;
import me.valour.bowls.models.User;

@SuppressLint("NewApi")
public class BowlsGroup extends FrameLayout {

    int mMeasuredWidth;
    int mMeasuredHeight;
    float centerX=0;
    float centerY=0;
    int tableRadius;
    int bowlRadius;
    boolean measuredScreen;
    boolean selectReady = false;
    boolean addRemovable = true;

    FrameLayout.LayoutParams defaultParams;
    BowlSelectListener selectListener;
    DeleteDropListener deleteListener;
    BowlsGroupAgent agent;

    LinkedList<BowlView> bowls;

    FrameLayout trashBowl;
    int bowlsIdCounter = 1;
    LinkedList<Integer> disusedIds;
    int currentDisusedId = -1;
    BowlView selectedForDelete;

    public BowlsGroup(Context context) {
        super(context);
        init();
    }

    public BowlsGroup(Context context, AttributeSet ats, int ds) {
        super(context, ats, ds);
        init();
    }

    public BowlsGroup(Context context, AttributeSet attr) {
        super(context, attr);

        init();
    }

    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom){
        measureView();

        double angleDelta = Math.PI*2.0/bowls.size();
        double topX = 0;
        double topY = -1.0*tableRadius;

        int i= 0;
        for(BowlView bowl: bowls){
            bowl.bringToFront();
            double angle = angleDelta*i;
            double px = Math.cos(angle)*topX - Math.sin(angle)*topY + centerX;
            double py = Math.sin(angle)*topX - Math.cos(angle)*topY + centerY;
            bowl.setAngle(angle);
            bowl.move((float)px, (float)py);
            i++;
        }

        super.onLayout(changed, left, top, right, bottom);
    }

    private void init() {
        selectListener = new BowlSelectListener();
        deleteListener = new DeleteDropListener();
        measuredScreen = false;
        defaultParams = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        setClickable(true);
        setFocusable(true);

        bowls = new LinkedList<BowlView>();
        for (int i = 1; i <= Kitchen.minBowls; i++) {
            BowlView bowl = new BowlView(this.getContext());
            bowl.setColors(Kitchen.assignColor(bowlsIdCounter));
            bowls.add(bowl);
            bowlsIdCounter++;
            this.addView(bowl, defaultParams);
            bowl.setOnTouchListener(selectListener);
        }

        disusedIds = new LinkedList<Integer>();

        trashBowl = new FrameLayout(this.getContext());
        //trashBowl.setBackgroundResource(R.drawable.ic_recycle);
        this.addView(trashBowl, defaultParams);
        trashBowl.setVisibility(View.GONE);
        trashBowl.setOnDragListener(deleteListener);
    }

    public void measureView(){
        if(measuredScreen){
            return;
        } else {
            mMeasuredWidth = getMeasuredWidth();
            mMeasuredHeight = getMeasuredHeight();
            int cx = mMeasuredWidth / 2;
            int cy = mMeasuredHeight / 2;
            tableRadius = Math.min(cx, cy);
            centerX = (float)cx;
            centerY = (float)cy;

            double q = ((double) tableRadius * 2.0 * Math.PI)
                    / (double) Kitchen.maxBowls;
            bowlRadius = (int) (q / 2.0);
            tableRadius -= bowlRadius;


            for(BowlView bv: bowls){
                bv.setRadius(bowlRadius);
            }
            measuredScreen = true;

            float s = (float)(2*tableRadius-4*bowlRadius);
            if((s*0.9)>2*bowlRadius){
                s *= 0.9;
            }
            trashBowl.setMinimumHeight((int)s);
            trashBowl.setMinimumWidth((int)s);
            trashBowl.setX(centerX-s/2);
            trashBowl.setY(centerY-s/2);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);
        int d = Math.min(measuredWidth, measuredHeight);
        setMeasuredDimension(d, d);

    }

    private int measure(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            result = 500;
        } else {
            result = specSize;
        }
        return result;
    }

    public void addRemoveIcons(boolean showAdd){
        if(showAdd){
            if(bowls.size()<Kitchen.maxBowls){

                trashBowl.setVisibility(View.GONE);
            }
        } else {

            trashBowl.setVisibility(View.VISIBLE);
        }
    }

    public void clearCenter(){

        trashBowl.setVisibility(View.GONE);
    }

    private BowlView getNewBowl(){
        BowlView bowl = new BowlView(this.getContext());
        //TODO: set color of new bowl
        if(measuredScreen){
            bowl.setRadius(bowlRadius);
        }
        bowl.move(centerX, centerY-(bowlRadius/2));
        this.addView(bowl, defaultParams);
        bowl.setOnTouchListener(selectListener);
        bowl.bringToFront();
        return bowl;
    }

    public void addBowl(){
        BowlView bowl = getNewBowl();

        if(currentDisusedId==-1){
            bowlsIdCounter++;
        } else {
            bowl.setId(currentDisusedId);
        }

        bowls.add(bowl);
        agent.addUser(bowl.user);
        bowl.formatText();

        if(disusedIds.isEmpty()){
            currentDisusedId = -1;

        } else {
            currentDisusedId = disusedIds.removeFirst();

        }

        if(bowls.size()>=Kitchen.maxBowls){
            //TODO: flag that no more new bowls can be added
        }
    }

    public void removeBowl(final BowlView bowl){
        disusedIds.add(bowl.getId());
        bowls.remove(bowl);
        refreshBowls();
        addRemoveIcons(true);

        if(currentDisusedId==-1 && !disusedIds.isEmpty()){
            currentDisusedId = disusedIds.removeFirst();

        }

        if((bowls.size()+1)==Kitchen.maxBowls){
           //TODO: allow bowl to be added again
        }
    }

    public void refreshBowls() {
        for(BowlView bv: bowls){
            bv.formatText();
            bv.invalidate();
        }
    }

    public List<Integer> getBowlViewIds() {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (BowlView bw : bowls) {
            ids.add(bw.getId());
        }
        return ids;
    }

    public List<User> getBowlUsers() {
        ArrayList<User> users = new ArrayList<User>();
        for (BowlView bw : bowls) {
            users.add(bw.user);
        }
        return users;
    }

    public void bowlsFocus(boolean unfade) {
        for (BowlView bv : bowls) {
            if (unfade) {
                bv.unfade();
            } else {
                bv.fade();
            }
        }
    }


    public void clearSelection(){
        selectListener.selected.clear();
        for(BowlView bv: bowls){
            bv.setSelected(false);
        }
    }

    public void readyBowlSelect(){
        selectReady = true;
        clearSelection();
        bowlsFocus(false);
        clearCenter();
    }

    public void stopBowlSelect(){
        selectReady = false;
        clearSelection();
        bowlsFocus(true);
        addRemoveIcons(true);
    }

    public List<User> getSelectedUsers(){
        return selectListener.selected;
    }

    public void manualSelect(List<User> users){
        selectListener.selected.addAll(users);
       /*
        for(User u: users){
            u.view.setSelected(true);
            u.view.unfade();
        }
        */
    }

    public void enableActions(){
        addRemovable = true;
    }

    public void disableActions(){
        selectReady = false;
        addRemovable = false;
    }

    public void showInfo(BowlView bv){

    }


    public void attachBowlAgents(Activity activity){
        agent = (BowlsGroupAgent) activity;
    }

    private class DeleteDropListener implements OnDragListener{

        public boolean deleteBowl(BowlView bowl){
            if(agent.removeUserConfirm(bowl)){
                removeBowl(bowl);
            }
            return true;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    //no action necessary
                    ((BowlView)event.getLocalState()).setVisibility(View.INVISIBLE);
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    break;
                case DragEvent.ACTION_DROP:
                    if(v.getId()!=-1){
                        return(false);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    if(v.getId()==-1){
                        final BowlView view = ((BowlView)event.getLocalState());
                        if(event.getResult()){
                            view.post(new Runnable(){
                                @Override
                                public void run() {
                                    clearCenter();
                                    deleteBowl(view);
                                }});

                        } else {
                            view.post(new Runnable(){
                                @Override
                                public void run() {
                                    view.setVisibility(View.VISIBLE);
                                    addRemoveIcons(true);
                                }});
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }

    }

    private class BowlSelectListener implements OnTouchListener{

        public List<User> selected;
        public boolean moved = false;
        public float px=0;
        public float py=0;

        public BowlSelectListener(){
            selected = new ArrayList<User>();
        }

        @Override
        public boolean onTouch(View v, MotionEvent move) {
            BowlView bv = (BowlView)v;
            int action = move.getAction();
            if(selectReady){
                if(action==MotionEvent.ACTION_DOWN){
                    if(bv.toggleSelected()){
                        selected.remove(bv.user);
                    } else {
                        selected.add(bv.user);
                    }
                } return false;
            } else if (bowls.size()>Kitchen.minBowls && addRemovable) {
                switch(action){
                    case MotionEvent.ACTION_DOWN:
                        moved = false;
                        px = move.getX();
                        py = move.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:

                        float movedX = Math.abs(move.getX() - px);
                        float movedY = Math.abs(move.getY() - py);

                        if(movedX>10 || movedY>10){
                            addRemoveIcons(false);
                            v.setOnDragListener(deleteListener);
                            ClipData data = ClipData.newPlainText("", "");
                            DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                            v.startDrag(data, shadowBuilder, v, 0);
                            moved = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if(!moved){ showInfo(bv); }
                        break;
                }
                return true;
            }
            else {
                if(action==MotionEvent.ACTION_UP){
                    showInfo(bv);
                }
                return true;
            }
        }

    }

    public interface BowlsGroupAgent{
        public void addUser(User user);
        public boolean removeUserConfirm(BowlView bv);
        public void removeUserDo(User user);
    }

}