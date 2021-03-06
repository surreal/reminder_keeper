package com.reminder_keeper.activities;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import com.reminder_keeper.adapters.AdapterERV.models.GroupItemModel;
import com.reminder_keeper.views.ToolbarView;
import com.reminder_keeper.adapters.AdapterERV.AdapterERV;
import com.reminder_keeper.AuthorityClass;
import com.reminder_keeper.CursorsDBMethods;
import com.reminder_keeper.data_base.DBOpenHelper;
import com.reminder_keeper.data_base.DBProvider;
import com.reminder_keeper.listeners.OnListItemClickListener;
import com.reminder_keeper.R;
import com.thoughtbot.expandablerecyclerview.listeners.GroupExpandCollapseListener;
import com.thoughtbot.expandablerecyclerview.listeners.OnGroupClickListener;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

public class TheArrangeActivity extends AppCompatActivity implements OnListItemClickListener, GroupExpandCollapseListener, OnGroupClickListener, DialogInterface.OnDismissListener {
    public static final String THE_ARRANGE_ACTIVITY = "TheArrangeActivity";
    private static RecyclerView recyclerView;
    private static AdapterERV adapterERV;
    private static boolean isGroupOnDrag;

    private static ArrayList<Integer> groupsFPosIdsMap;
    private static ArrayList<Integer> childrenFlatPositions;
    private static ArrayList<String> groupsToExpandTitles;

    public static boolean isOnDrug;
    private Cursor cursor;
    private static CursorsDBMethods cursorsDBMethods;
    private static TheArrangeActivity activity;
    private static boolean isForDelete;
    public static String clickedElementString;
    public final static String xElement = "X_ELEMENT";
    public final static String titleTVElement = "TITLE_TV_ELEMENT";
    private int counter = 0;
    private ArrayList<GroupItemModel> groupsLC;
    private int clickedGroupFlatPos;
    private MainActivity mainActivity;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {   super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_folders);
        activity = this;
        mainActivity = new MainActivity();
        groupsToExpandTitles = new ArrayList<>();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_custom));
        new ToolbarView(this, getSupportActionBar(), THE_ARRANGE_ACTIVITY);
        setRecyclerView();
        initAdapter(false);
        initDragFoldersAndLists();
        cursorsDBMethods = new CursorsDBMethods(this);

        childrenFlatPositions = new ArrayList<>();
        adapterERV.setOnGroupExpandCollapseListener(this);
        adapterERV.setOnGroupClickListener(this);
    }

    //TODO: init RecyclerView
    public void setRecyclerView()
    {
        recyclerView = (RecyclerView) findViewById(R.id.activity_edit_folders_recycler_view_folders_and_lists);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    //TODO load / reload adapters
    public void initAdapter(boolean isForDelete)
    {
        this.isForDelete = isForDelete;
        groupsLC = new AuthorityClass().loadGroupsChildrenAndListsForERVAdapter(activity);
        adapterERV = new AdapterERV(groupsLC, activity, THE_ARRANGE_ACTIVITY, isForDelete);
        recyclerView.setAdapter(adapterERV);

        //if (recyclerView.isComputingLayout()){ }
        try{
            adapterERV.notifyDataSetChanged();
        } catch (Exception e){
            //TODO: potential error put an eye on it
            Log.d("tryCatchException", "e.getMessage() == " + e.getMessage() + "\n\n");
            Log.d("tryCatchException", "e.getCause() == " + e.getCause() + "\n\n");
            e.getMessage();
        }
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() { }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.icon).setIcon(R.mipmap.x).setVisible(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_layer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                nullTheArrays();
                setResult(AuthorityClass.RESULT_LOAD_ARRAYS);
                finish();
                break;
            case R.id.icon:
                isForDelete = !isForDelete;
                addExpandedToArray();
                if (isForDelete) {
                    item.setIcon(R.mipmap.v);
                    initAdapter(true);
                } else {
                    item.setIcon(R.mipmap.x);
                    initAdapter(false);
                }
                expandRelevantGroup();
        }
        return super.onOptionsItemSelected(item);
    }

    //TODO: DRAG FOLDERS AND LISTS
    private void initDragFoldersAndLists()
    {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0)
        {
            private int targetItemCurrentPosition = 0;
            private int moveToPosition = 0;
            private int onMoveCounter = 0;
            private int targetItemStartPosition = 0;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
            {
                isOnDrug = true;
                onMoveCounter++;
                if (onMoveCounter == 1)
                {
                    targetItemStartPosition = viewHolder.getAdapterPosition();
                }
                targetItemCurrentPosition = viewHolder.getAdapterPosition();
                moveToPosition = target.getAdapterPosition();

                if ((childrenFlatPositions.size() == 0) ||
                        (isGroupOnDrag && !AdapterERV.childrenFPosId.containsKey(moveToPosition)) ||
                        (!isGroupOnDrag && AdapterERV.childrenFPosId.containsKey(moveToPosition)))
                {
                    adapterERV.notifyItemMoved(targetItemCurrentPosition, moveToPosition);
                }
                return false;
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
            {   super.clearView(recyclerView, viewHolder);
                if (onMoveCounter > 0)
                {
                    if (isGroupOnDrag)
                    {
                        checkDirectionRunReorganize(adapterERV.getGroupId(targetItemStartPosition), adapterERV.getGroupId(moveToPosition), true);
                    } else {
                        if (AdapterERV.childrenFPosId.containsKey(targetItemStartPosition) && AdapterERV.childrenFPosId.containsKey(moveToPosition))
                        {
                            updateChildrenDB(adapterERV.getChildId(targetItemStartPosition), adapterERV.getChildId(moveToPosition), -1);
                        } else if (AdapterERV.childrenFPosId.containsKey(targetItemStartPosition) && AdapterERV.groupsFPosId.containsKey(moveToPosition))
                        {
                            updateChildrenDB(adapterERV.getChildId(targetItemStartPosition), adapterERV.getChildId(moveToPosition), adapterERV.getGroupId(moveToPosition));
                        }
                    }
                    AdapterERV.isConstructorWithExpand = true;
                    initAdapter(isForDelete);
                    expandRelevantGroup();
                    onMoveCounter = 0;
                }
                isOnDrug = false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    //TODO: update DB after item moved
    private void checkDirectionRunReorganize(int targetItemId, int toPositionId, boolean isGroup)
    {
        //TODO: moving UP
        if (targetItemId < toPositionId)
        {
            reorganizeDBPositions(targetItemId, toPositionId, true, isGroup);
        } //TODO: moving DOWN
        else {
            reorganizeDBPositions(targetItemId, toPositionId, false, isGroup);
        }
    }

    //TODO: reorganize DB positions
    private void reorganizeDBPositions(int targetItemId, int toPositionId, boolean movingUp, boolean isGroup)
    {
        ContentValues contentValues = new ContentValues();
        cursorsDBMethods.getCursorGroupsLists();
        cursorsDBMethods.getCursorChildren();
        cursor = isGroup ? CursorsDBMethods.cursorGroupsLists : CursorsDBMethods.cursorChildren;
        cursor.moveToFirst();
        int tempId = cursor.getInt(cursor.getColumnIndex(DBOpenHelper.COLUMN_ID)) +1;
        contentValues.put(DBOpenHelper.COLUMN_ID, tempId);

        Uri uriGroupOrChildrenTable = isGroup ? DBProvider.GROUPS_TABLE_PATH_URI : DBProvider.CHILDREN_TABLE_PATH_URI;
        getContentResolver().update(uriGroupOrChildrenTable, contentValues, DBOpenHelper.COLUMN_ID + "=" + targetItemId, null);
        if (movingUp)
        {
            cursorMoveToLastPositionPlusOne();
            while (cursor.moveToPrevious())
            {
                int id = cursor.getInt(cursor.getColumnIndex(DBOpenHelper.COLUMN_ID));
                if (id > targetItemId && id <= toPositionId)
                {
                    contentValues.put(DBOpenHelper.COLUMN_ID, id -1);
                    getContentResolver().update(uriGroupOrChildrenTable, contentValues, DBOpenHelper.COLUMN_ID + "=" + id, null);
                }
            }
        } else {
            cursorMoveToFirstPositionMinusOne();
            while (cursor.moveToNext())
            {
                int id = cursor.getInt(cursor.getColumnIndex(DBOpenHelper.COLUMN_ID));
                if (id >= toPositionId && id < targetItemId)
                {
                    contentValues.put(DBOpenHelper.COLUMN_ID, id +1);
                    getContentResolver().update(uriGroupOrChildrenTable, contentValues, DBOpenHelper.COLUMN_ID + "=" + id, null);
                }
            }
        }
        contentValues.put(DBOpenHelper.COLUMN_ID, toPositionId);
        getContentResolver().update(uriGroupOrChildrenTable, contentValues, DBOpenHelper.COLUMN_ID + "=" + tempId, null);
    }

    private void cursorMoveToLastPositionPlusOne()
    {
        cursor.moveToLast();
        cursor.moveToNext();
    }
    private void cursorMoveToFirstPositionMinusOne()
    {
        cursor.moveToFirst();
        cursor.moveToPrevious();
    }

    private void updateChildrenDB(int targetItemChildId, int toPositionChildId, int toPositionGroupId)
    {
        String whereTargetRow = DBOpenHelper.COLUMN_ID + "=" + targetItemChildId;
        String whereToRow;
        boolean isList = false;
        Cursor cursorToPosition = null;
        ContentValues contentValues = new ContentValues();

        if (toPositionGroupId != -1) {
            whereToRow = DBOpenHelper.COLUMN_ID + "=" + toPositionGroupId;
            cursorToPosition = getContentResolver().query(DBProvider.GROUPS_TABLE_PATH_URI, null, whereToRow, null, null);
            cursorToPosition.moveToFirst();
            String toPositionGroupTitle = cursorToPosition.getString(cursorToPosition.getColumnIndex(DBOpenHelper.COLUMN_GROUP));
            isList = toPositionGroupTitle == null;
        } else if (toPositionChildId != -1) {
            whereToRow = DBOpenHelper.COLUMN_ID + "=" + toPositionChildId;
            cursorToPosition = getContentResolver().query(DBProvider.CHILDREN_TABLE_PATH_URI, null, whereToRow, null, null);
            cursorToPosition.moveToFirst();
        }

        if (!isList)
        {
            Cursor cursorTargetItem = getContentResolver().query(DBProvider.CHILDREN_TABLE_PATH_URI, null, whereTargetRow, null, null);
            cursorTargetItem.moveToFirst();
            String targetItemChildTitle = cursorTargetItem.getString(cursorTargetItem.getColumnIndex(DBOpenHelper.COLUMN_CHILD));
            String targetItemGroupTitle = cursorTargetItem.getString(cursorTargetItem.getColumnIndex(DBOpenHelper.COLUMN_GROUP));
            String toPositionGroupTitle = cursorToPosition.getString(cursorToPosition.getColumnIndex(DBOpenHelper.COLUMN_GROUP));
            if (toPositionGroupId != -1){
                groupsToExpandTitles.add(toPositionGroupTitle);
            }

            if (targetItemGroupTitle.equals(toPositionGroupTitle))
            {
                checkDirectionRunReorganize(targetItemChildId, toPositionChildId, false);
            } else {
                contentValues.put(DBOpenHelper.COLUMN_GROUP, toPositionGroupTitle);
                contentValues.put(DBOpenHelper.COLUMN_CHILD, targetItemChildTitle);
                getContentResolver().insert(DBProvider.CHILDREN_TABLE_PATH_URI, contentValues);
                getContentResolver().delete(DBProvider.CHILDREN_TABLE_PATH_URI, whereTargetRow, null);
            }
        }
    }

    //TODO: on expand collapse methods
    /** passing from adapter maps with items id's and flat positions */

    //TODO: onLongClick collapse relevant Groups
    public void onLongClick(String title, boolean isGroupOnDrag)
    {
        this.isGroupOnDrag = isGroupOnDrag;

        if (isGroupOnDrag)
        {
            groupsToExpandTitles = new ArrayList<>();
            //TODO: add to array expanded group titles that not equals to clicked group
            for (int i = 0; i < adapterERV.getGroups().size(); i++)
            {
                //TODO: if group on i is expanded
                if (adapterERV.isGroupExpanded(adapterERV.getGroups().get(i)))
                {
                    //TODO: if clicked group title not equal to i
                    if (!adapterERV.getGroups().get(i).getTitle().equals(title))
                    {
                        //TODO: add to array
                        groupsToExpandTitles.add(adapterERV.getGroups().get(i).getTitle());
                    }
                }
                //TODO: if clicked group title equal to i
                if (adapterERV.getGroups().get(i).getTitle().equals(title))
                {
                    //TODO: start new loop from i
                    for (int j = i; j < adapterERV.getGroups().size(); j++)
                    {
                        //TODO: collapse group if expanded
                        if (adapterERV.isGroupExpanded(adapterERV.getGroups().get(j)))
                        {
                            adapterERV.toggleGroup(adapterERV.getGroups().get(j));
                        }
                    }
                    break;
                }
            }
        } else {
            addExpandedToArray();
        }
    }

    //TODO: add expanded group titles to array
    public void addExpandedToArray()
    {
        groupsToExpandTitles = new ArrayList<>();
        for (int i = 0; i < adapterERV.getGroups().size(); i++)
        {
            if (adapterERV.isGroupExpanded(adapterERV.getGroups().get(i)))
            {
                groupsToExpandTitles.add(adapterERV.getGroups().get(i).getTitle());
            }
        }
    }

    //TODO: expand relevant group
    public void expandRelevantGroup()
    {
        for (int i = 0; i < adapterERV.getGroups().size(); i++)
        {
            if (groupsToExpandTitles.contains(adapterERV.getGroups().get(i).getTitle()))
            {
                adapterERV.toggleGroup(adapterERV.getGroups().get(i));
            }
        }
    }

    //TODO: called from AdapterERV (on item click listener)
    @Override
    public void itemClicked(final String groupTitle, final String childTitle, final String listTitle, String passedFrom, boolean isForAction, final int id)
    {
        if (isForAction)
        {
            if (clickedElementString.equals(titleTVElement)) {
                initChangeTitleView(groupTitle, childTitle, listTitle, id);
            } else if (clickedElementString.equals(xElement)) {
                initDeleteConfirmationView(groupTitle, childTitle, listTitle, id);
            }
        }
    }

    private void initDeleteConfirmationView(final String groupTitle, final String childTitle, final String listTitle, int id)
    {
        if (alertDialog == null)
        {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            alertDialog = dialogBuilder.create();
            View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_confirm_delete, null, false);
            TextView deleteConfirmationBTN = (TextView) dialogView.findViewById(R.id.dialog_confirm_delete_remove_button_tv);
            TextView deleteConfirmationTV = (TextView) dialogView.findViewById(R.id.dialog_confirm_delete_msg_tv);
            deleteConfirmationBTN.setText(getResources().getString(R.string.delete));

            int listsInGroupCount = 0;
            if (groupTitle != null && childTitle == null) {
                for (int i = 0; i < adapterERV.getGroups().size(); i++) {
                    if (adapterERV.getGroups().get(i).getTitle().equals(groupTitle)) {
                        listsInGroupCount = adapterERV.getGroups().get(i).getItemCount();
                    }
                }

                String folderMsg = getString(R.string.the_folder)
                        + " '" + groupTitle + "'" + "\n"
                        + getString(R.string.including) + ": \n"
                        + getString(R.string.lists) + " - " + listsInGroupCount + "," + "\n"
                        + getString(R.string.reminders) + " - " + countOfReminders(groupTitle, childTitle, listTitle) + "." + "\n"
                        + getString(R.string.remove_this_folder_and_all_reminders_inside);
                deleteConfirmationTV.setText(folderMsg);

            } else if (listTitle != null || childTitle != null) {
                String title = listTitle != null ? listTitle : childTitle;
                String listMsg = getString(R.string.the_list)
                        + " '" + title + "'\n"
                        + getString(R.string.including) + ": \n"
                        + getString(R.string.reminders) + " - " + countOfReminders(groupTitle, childTitle, listTitle) + "." + "\n"
                        + getString(R.string.remove_this_list_and_all_reminders_inside);
                deleteConfirmationTV.setText(listMsg);
            }

            alertDialog.setView(dialogView);
            alertDialog.show();
            alertDialog.setOnDismissListener(this);
            deleteConfirmationBTN.setOnClickListener(initOnElementsClickListener(groupTitle, childTitle, listTitle, null, id));
        }
    }

    private int countOfReminders(final String groupTitle, final String childTitle, final String listTitle)
    {
        counter = 0;
        cursor = cursorsDBMethods.getCursorToDo();
        cursor = CursorsDBMethods.cursor;
        String currentGroup, currentChild, currentList;
        while (cursor.moveToNext())
        {
            currentGroup = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_GROUP));
            currentChild = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_CHILD));
            currentList = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_LIST));
            if (((currentChild != null && childTitle != null && childTitle.equals(currentChild)) && groupTitle.equals(currentGroup)) ||
                    (listTitle != null && currentList != null && currentList.equals(listTitle)) ||
                    (groupTitle != null && currentGroup != null && currentGroup.equals(groupTitle)))
            { counter++; }
        }
        return counter;
    }

    private void initChangeTitleView(final String groupTitle, final String childTitle, final String listTitle, final int id)
    {
        if (alertDialog == null){
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            alertDialog = dialogBuilder.create();
            View changeTitleView = LayoutInflater.from(this).inflate(R.layout.dialog_new_folder, null, true);
            final EditText changeTitleET = (EditText) changeTitleView.findViewById(R.id.input_title_view_title_et);
            Button updateTitleBtn = (Button) changeTitleView.findViewById(R.id.input_title_view_button);
            updateTitleBtn.setText(R.string.update_title);
            changeTitleET.setText(getRelevantTitle(groupTitle, childTitle, listTitle));
            alertDialog.setView(changeTitleView);
            updateTitleBtn.setOnClickListener(initOnElementsClickListener(groupTitle, childTitle, listTitle, changeTitleET, id));
            alertDialog.setOnDismissListener(this);
            alertDialog.show();
        }
    }

    private String getRelevantTitle(final String groupTitle, final String childTitle, final String listTitle)
    {
        String title = "";
        if (childTitle != null)
        { title = childTitle; }
        else if (listTitle != null)
        { title = listTitle; }
        else if (groupTitle != null)
        { title = groupTitle; }
        return title;
    }

    private View.OnClickListener initOnElementsClickListener(final String groupTitle, final String childTitle, final String listTitle, final EditText changeTitleET, final int id)
    {
        View.OnClickListener onElementsClickListener = new View.OnClickListener()
        { @Override
            public void onClick(View view)
            {
                if (clickedElementString.equals(titleTVElement))
                {
                    String newTitle = changeTitleET.getText().toString();
                    String keyDB;
                    ContentValues contentValues = new ContentValues();
                    Uri uri;
                    if (childTitle != null)
                    {
                        cursorsDBMethods.getCursorChildren(id);
                        cursor = CursorsDBMethods.cursorChildren;
                        cursor.moveToFirst();
                        keyDB = DBOpenHelper.COLUMN_CHILD;
                        uri = DBProvider.CHILDREN_TABLE_PATH_URI;
                        contentValues.put(DBOpenHelper.COLUMN_GROUP, groupTitle);
                    } else {
                        cursorsDBMethods.getCursorGroupsLists(id);
                        cursor = CursorsDBMethods.cursorGroupsLists;
                        cursor.moveToFirst();
                        keyDB = listTitle == null ? DBOpenHelper.COLUMN_GROUP : DBOpenHelper.COLUMN_LIST;
                        uri = DBProvider.GROUPS_TABLE_PATH_URI;
                    }
                    contentValues.put(keyDB, newTitle);
                    getContentResolver().update(uri, contentValues,DBOpenHelper.COLUMN_ID + "=" + id, null);

                    /* if change group title run on children table containing this group name update with renamed */
                    if (childTitle != null)
                    {
                        String columnProjection = DBOpenHelper.COLUMN_CHILD;
                        uri = DBProvider.TODO_TABLE_PATH_URI;
                        updateTableValues(childTitle, newTitle, uri, columnProjection);

                        uri = DBProvider.CHECKED_TABLE_PATH_URI;
                        updateTableValues(childTitle, newTitle, uri, columnProjection);
                    } else if (groupTitle != null)
                    {
                        String columnProjection = DBOpenHelper.COLUMN_GROUP;
                        uri = DBProvider.CHILDREN_TABLE_PATH_URI;
                        updateTableValues(groupTitle, newTitle, uri, columnProjection);

                        uri = DBProvider.TODO_TABLE_PATH_URI;
                        updateTableValues(groupTitle, newTitle, uri, columnProjection);

                        uri = DBProvider.CHECKED_TABLE_PATH_URI;
                        updateTableValues(groupTitle, newTitle, uri, columnProjection);
                    } else if (listTitle != null)
                    {
                        String columnProjection = DBOpenHelper.COLUMN_LIST;

                        uri = DBProvider.TODO_TABLE_PATH_URI;
                        updateTableValues(listTitle, newTitle, uri, columnProjection);

                        uri = DBProvider.CHECKED_TABLE_PATH_URI;
                        updateTableValues(listTitle, newTitle, uri, columnProjection);
                    }
                    addExpandedToArray();
                    if (groupsToExpandTitles.size() > 0 && groupsToExpandTitles.indexOf(groupTitle) != -1 && groupTitle != null && childTitle == null){ groupsToExpandTitles.set(groupsToExpandTitles.indexOf(groupTitle), changeTitleET.getText().toString()); }
                    initAdapter(isForDelete);
                    alertDialog.dismiss();
                } else if (clickedElementString.equals(xElement))
                {
                    if (listTitle != null) {
                        moveRemindersToRB(listTitle, null, null, DBProvider.TODO_TABLE_PATH_URI, DBOpenHelper.COLUMN_LIST);
                        moveRemindersToRB(listTitle,  null, null, DBProvider.CHECKED_TABLE_PATH_URI, DBOpenHelper.COLUMN_LIST);
                    } else if (childTitle != null) {
                        moveRemindersToRB(null, groupTitle, childTitle, DBProvider.TODO_TABLE_PATH_URI, DBOpenHelper.COLUMN_CHILD);
                        moveRemindersToRB(null, groupTitle, childTitle, DBProvider.CHECKED_TABLE_PATH_URI, DBOpenHelper.COLUMN_CHILD);
                    } else if (groupTitle != null) {
                        moveRemindersToRB(null, groupTitle, null, DBProvider.TODO_TABLE_PATH_URI, DBOpenHelper.COLUMN_GROUP);
                        moveRemindersToRB(null, groupTitle, null, DBProvider.CHECKED_TABLE_PATH_URI, DBOpenHelper.COLUMN_GROUP);
                        findAndRemoveFromChildDB(groupTitle);
                    }
                    Uri uri = childTitle != null ? DBProvider.CHILDREN_TABLE_PATH_URI : DBProvider.GROUPS_TABLE_PATH_URI;
                    activity.getContentResolver().delete(uri, DBOpenHelper.COLUMN_ID + "=" + id, null);
                    addExpandedToArray();
                    initAdapter(isForDelete);
                    alertDialog.dismiss();
                }
                expandRelevantGroup();
            }
        };
        return onElementsClickListener;
    }

    private void findAndRemoveFromChildDB(String title)
    {
        cursor = activity.getContentResolver().query(DBProvider.CHILDREN_TABLE_PATH_URI, null, null,null,null);
        while(cursor.moveToNext())
        {
            int id = cursor.getInt(cursor.getColumnIndex(DBOpenHelper.COLUMN_ID));
            String group = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_GROUP));

            if ((group != null && group.equals(title)))
            {
                activity.getContentResolver().delete(DBProvider.CHILDREN_TABLE_PATH_URI, DBOpenHelper.COLUMN_ID + "=" + id, null);
            }
        }
    }

    private void moveRemindersToRB(String listTitle, String groupTitle, String childTitle, Uri uri, String columnIndex)
    {
        cursor = activity.getContentResolver().query(uri, null, null,null,null);
        String title = "";
        if (childTitle != null) {
            title = childTitle;
        } else if (listTitle != null) {
            title = listTitle;
        } else if (groupTitle != null) {
            title = groupTitle;
        }
        while(cursor.moveToNext())
        {
            int id = cursor.getInt(cursor.getColumnIndex(DBOpenHelper.COLUMN_ID));
            String list = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_LIST));
            String group = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_GROUP));
            String child = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_CHILD));
            String notification = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_REMINDER));
            String timeDate = cursor.getString(cursor.getColumnIndex(DBOpenHelper.COLUMN_DATE_TIME));

            String relevantColumnTitle = cursor.getString(cursor.getColumnIndex(columnIndex));

            if ((relevantColumnTitle != null && relevantColumnTitle.equals(title)))
            {
                if ((childTitle != null && groupTitle.equals(group) && childTitle.equals(child)) ||
                        (listTitle != null && listTitle.equals(list)) ||
                        (groupTitle != null && groupTitle.equals(group)))
                {
                    cursorsDBMethods.moveToRecyclingBin(group, child, list, notification, timeDate);
                    activity.getContentResolver().delete(uri, DBOpenHelper.COLUMN_ID + "=" + id, null);
                }
                if (uri == DBProvider.TODO_TABLE_PATH_URI) {
                    mainActivity.deleteAlarmNotification(id);
                }
            }
        }
    }

    private void updateTableValues(String title, String newTitle, Uri uri, String columnProjection)
    {
        cursor = getContentResolver().query(uri, new String[]{columnProjection, DBOpenHelper.COLUMN_ID}, null, null, null);
        ContentValues contentValues = new ContentValues();
        while (cursor.moveToNext())
        {
            String boundGroup = cursor.getString(cursor.getColumnIndex(columnProjection));
            int id = cursor.getInt(cursor.getColumnIndex(DBOpenHelper.COLUMN_ID));
            if (boundGroup != null && boundGroup.equals(title))
            {
                contentValues.put(columnProjection, newTitle);
                getContentResolver().update(uri, contentValues, DBOpenHelper.COLUMN_ID + "=" +id, null);
            }
        }
    }

    private void nullTheArrays()
    {
        childrenFlatPositions = null;
        groupsFPosIdsMap = null;
        groupsToExpandTitles = null;
    }

    @Override
    public void onBackPressed()
    {   super.onBackPressed();
        setResult(AuthorityClass.RESULT_LOAD_ARRAYS);
        finish();
        nullTheArrays();
    }

    @Override
    public void onGroupExpanded(ExpandableGroup group)
    {
        /* append count of clicked group items to currently opened group items */
        if (childrenFlatPositions.size() > 0)
        {
            ArrayList tempAL = new ArrayList<>();
            int min = Collections.min(childrenFlatPositions);
            if (clickedGroupFlatPos < min)
            {
                for (int i = 0; i < childrenFlatPositions.size(); i++)
                {
                    tempAL.add(childrenFlatPositions.get(i) +group.getItemCount());

                }
                childrenFlatPositions = tempAL;
            }
        }
        /* generate clicked group children positions and add to childrenFlatPositions[] */
        for (int i = clickedGroupFlatPos +1; i < clickedGroupFlatPos +1 +group.getItemCount(); i++) { childrenFlatPositions.add(i); }
    }

    @Override
    public void onGroupCollapsed(ExpandableGroup group)
    {
        /* generate clicked group children positions and remove from childrenFlatPositions[] */
        for (int i = clickedGroupFlatPos +1; i < clickedGroupFlatPos +1 +group.getItemCount(); i++)
        { if (childrenFlatPositions.contains(i)){ childrenFlatPositions.remove(childrenFlatPositions.indexOf(i)); } }

        /* unAppend count of clicked group items to currently opened group items */
        if (childrenFlatPositions.size() > 0)
        {
            ArrayList tempAL = new ArrayList<>();
            int min = Collections.min(childrenFlatPositions);
            if (clickedGroupFlatPos < min)
            {
                for (int i = 0; i < childrenFlatPositions.size(); i++)
                {
                    tempAL.add(childrenFlatPositions.get(i) -group.getItemCount());
                }
                childrenFlatPositions = tempAL;
            }
        }
    }

    @Override
    public boolean onGroupClick(int flatPos)
    {
        clickedGroupFlatPos = flatPos;
        return true;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) { alertDialog = null; }
}
