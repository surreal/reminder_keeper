package com.reminder_keeper.views;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.reminder_keeper.activities.MainActivity;
import com.reminder_keeper.activities.ReminderActivity;
import com.reminder_keeper.adapters.AdapterERV.AdapterERV;
import com.reminder_keeper.CursorsDBMethods;
import com.reminder_keeper.adapters.AdapterERV.models.GroupItemModel;
import com.reminder_keeper.listeners.NewListBtnClickListener;
import com.reminder_keeper.R;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.ArrayList;

public class SelectListView implements DialogInterface.OnDismissListener
{
    private static RecyclerView recyclerViewSelectListView;
    public static AlertDialog selectListViewDialog;
    private CursorsDBMethods cursors;
    private Activity activity;
    private String requestFrom;
    private final MainActivity mainActivity;
    private ArrayList<GroupItemModel> groupItemModels;
    private AdapterERV adapterERV;

    public SelectListView(Activity activity, String requestFrom)
    {
        this.activity = activity;
        this.requestFrom = requestFrom;
        cursors = new CursorsDBMethods(activity);
        mainActivity = new MainActivity();
    }

    public void initAdapter()
    {
        recyclerViewSelectListView.setLayoutManager(new LinearLayoutManager(activity));
        groupItemModels = mainActivity.loadGroupsChildrenAndListsForERVAdapter(activity);
        adapterERV = new AdapterERV(groupItemModels, activity, requestFrom, false);
        recyclerViewSelectListView.setAdapter(adapterERV);
        //expandRelevantGroup();
    }

    //TODO: init selectList View
    public void initListViewDialog()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        selectListViewDialog = alertDialogBuilder.create();
        final View selectListView = LayoutInflater.from(activity).inflate(R.layout.dialog_select_list, null, false);
        selectListViewDialog.setView(selectListView);
        recyclerViewSelectListView = (RecyclerView) selectListView.findViewById(R.id.select_list_view_recycler_view);
        Button newListButton = (Button) selectListView.findViewById(R.id.select_list_view_create_new_list_button);
        newListButton.setOnClickListener(new NewListBtnClickListener(activity, requestFrom));

        initAdapter();
        final LinearLayout unclassifiedLLayout = (LinearLayout) selectListView.findViewById(R.id.select_list_view_layout_unclassified);
        if (requestFrom.equals(ReminderActivity.REMINDER_ACTIVITY))
        {
            adapterERV.selectUnselectItemView(unclassifiedLLayout);
        }
        unclassifiedLLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestFrom.equals(MainActivity.MAIN_ACTIVITY))
                {
                    MainActivity.selectedGroupTitleSLV = null;
                    MainActivity.selectedChildTitleSLV = null;
                    MainActivity.selectedListTitleSLV = activity.getString(R.string.unclassified);
                    itemClickedInSLV();
                } else if (requestFrom.equals(ReminderActivity.REMINDER_ACTIVITY))
                {
                    ToolbarView.titleTV.setText(R.string.unclassified);
                    ReminderActivity.selectedGroupTitle = null;
                    ReminderActivity.selectedChildTitle = null;
                    ReminderActivity.selectedListTitle = activity.getString(R.string.unclassified);
                    adapterERV.selectUnselectItemView(unclassifiedLLayout);
                    selectListViewDialog.dismiss();
                }
            }
        });
        selectListViewDialog.setOnDismissListener(this);

        expandRelevantGroup();

        selectListViewDialog.show();
    }

    public void expandRelevantGroup() {
        if(requestFrom.equals(ReminderActivity.REMINDER_ACTIVITY)){
            for (ExpandableGroup expandableGroup : adapterERV.getGroups()){
                if(ReminderActivity.selectedGroupTitle != null && ReminderActivity.selectedGroupTitle.equals(expandableGroup.getTitle()))
                {
                    //if (ReminderActivity.childTitleDB != null){ ReminderActivity.selectedChildTitle = ReminderActivity.childTitleDB; }
                    adapterERV.toggleGroup(expandableGroup);
                }
            }
        }
    }

    public void itemClickedInSLV()
    {
        cursors.moveToDB(MainActivity.idToDoReminderItem, MainActivity.idCheckedReminderItem, false);
        mainActivity.initRelevantModeAdapter();
        selectListViewDialog.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface)
    {
        if (requestFrom.equals(MainActivity.MAIN_ACTIVITY))
        {
            mainActivity.initRelevantModeAdapter();
        }
    }
}
