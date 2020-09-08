import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import xzr.La.systemtoolbox.modules.java.LModule;
import xzr.La.systemtoolbox.ui.StandardCard;
import xzr.La.systemtoolbox.ui.views.LSpinner;
import xzr.La.systemtoolbox.utils.process.ShellUtil;

import java.util.ArrayList;
import java.util.List;

public class SchedBoost implements LModule {
    static final String TAG="SchedBoost";
    static final String node="/proc/sys/kernel/sched_boost";
    static final String advance_check_node="/dev/stune/schedtune.sched_boost_no_override";
    boolean is_running;
    boolean should_run;
    int current;
    LSpinner lSpinner;

    List<String> common=new ArrayList(){{
        add("小核优先负载");
        add("大核优先负载");
    }};

    List<String> advance=new ArrayList(){{
        add("小核优先负载");
        add("大核优先负载");
        add("大核优先负载（仅优先迁移的调度组）");
        add("小核优先负载+更激进的升频");
    }};

    @Override
    public String classname() {
        return "cpua";
    }

    @Override
    public View init(Context context) {
        if(no_compatibility())
            return null;
        Log.e(TAG,"Started");
        LinearLayout linearLayout=new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView title= StandardCard.title(context);
        title.setText("负载优先级配置");
        linearLayout.addView(title);
        TextView subtitle= StandardCard.subtitle(context);
        subtitle.setText("您可以在此配置任务在默认情况下将如何被分配至CPU");
        linearLayout.addView(subtitle);

        boolean advance_support=ShellUtil.run("if [ -f "+advance_check_node+" ]\nthen\necho true\nfi\n",true).equals("true");

        if(advance_support)
            lSpinner=new LSpinner(context,advance);
        else
            lSpinner=new LSpinner(context,common);

        linearLayout.addView(lSpinner);

        {
            TextView textView = new TextView(context);
            textView.setText("* 这个节点的值可能会被用户空间升频程序或是内核内部的调整程序改变");
            linearLayout.addView(textView);
        }

        lSpinner.setOnItemClickListener(new LSpinner.OnItemClickListener() {
            @Override
            public void onClick(int i) {
                ShellUtil.run("echo "+i+" > "+node,true);
            }
        });

        should_run=true;
        if(!is_running)
            new refresher().start();


        return linearLayout;
    }

    class refresher extends Thread{
        public void run(){
            Log.e(TAG,"Started2");
            is_running=true;
            try {
                while (should_run) {
                    current=Integer.parseInt(ShellUtil.run("cat " + node, true));

                    lSpinner.setSelection(current);
                    Thread.sleep(1000);
                }
            }
            catch (Exception e){

            }
            Log.e(TAG,"Done");
            is_running=false;
        }
    }

    boolean no_compatibility(){
        if(!ShellUtil.run("if [ -f "+node+" ]\nthen\necho true\nfi\n",true).equals("true"))
            return true;
        return false;
    }

    @Override
    public String onBootApply() {
        return "echo "+lSpinner.getSelection()+" > "+node+"\n";
    }

    @Override
    public void onExit() {
        should_run=false;
    }
}
