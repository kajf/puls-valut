package ch.prokopovi.ui.main;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

import ch.prokopovi.R;
import ch.prokopovi.db.BestRatesTable;
import ch.prokopovi.exported.DbColumn;
import ch.prokopovi.exported.RatesPlacesTable;
import ch.prokopovi.struct.Master;

/**
 * Created by Pavel_Letsiaha on 25-Mar-15.
 */
class BestListAdapter extends CursorAdapter {

    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat(
            "#.## км");
    private static final String LOG_TAG = "BestListAdapter";

    private class ViewHolder {

        ImageView expandCollapse;
        LinearLayout expandCollapseArea;

        TextView rateValue;
        TextView distance;
        TextView timeUpdated;

        TextView description;
        TextView address;
        TextView workHours;
        TextView phones;

        Button showConverter;
        ImageButton showMap;
    }

    private LayoutInflater mInflater;

    BestRatesFragment fragment;

    public BestListAdapter(BestRatesFragment fragment) {
        // empty cursor until update is performed
        super(fragment.getActivity(), null, 0);

        this.fragment = fragment;
        mInflater = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Log.d(LOG_TAG, "newView " + cursor.getPosition());

        View view = mInflater.inflate(R.layout.best_item_layout, parent, false);

        ViewHolder holder = new ViewHolder();
        holder.expandCollapse = (ImageView) view.findViewById(R.id.iv_expand_collapse);
        holder.expandCollapseArea = (LinearLayout) view.findViewById(R.id.item_best_expand);
        holder.rateValue = (TextView) view.findViewById(R.id.tv_item_best_rate_value);
        holder.distance = (TextView) view.findViewById(R.id.tv_item_best_rate_distance);
        holder.timeUpdated = (TextView) view.findViewById(R.id.tv_item_best_rate_time);
        holder.description = (TextView) view.findViewById(R.id.tv_item_best_description);
        holder.address = (TextView) view.findViewById(R.id.tv_item_best_addr);
        holder.workHours = (TextView) view.findViewById(R.id.tv_item_best_wh);
        holder.phones = (TextView) view.findViewById(R.id.tv_item_best_phones);

        holder.showConverter = (Button) view.findViewById(R.id.b_converter);
        holder.showMap = (ImageButton) view.findViewById(R.id.ib_show_map);

        view.setTag(holder);

        UiHelper.applyFont(context, view, null);

        return view;
    }

    @Override
    public void bindView(View rowView, Context context, Cursor cursor) {
        Log.d(LOG_TAG, "bindView " + cursor.getPosition());

        ViewHolder holder = (ViewHolder) rowView.getTag();

        fillRow(holder, cursor);

        final ImageView ivExpandCollapse = holder.expandCollapse;
        final View whView = holder.workHours;
        final View phonesView = holder.phones;

        // reset expand/collapse visibility
        ivExpandCollapse.clearAnimation();
        whView.setVisibility(View.GONE);
        phonesView.setVisibility(View.GONE);
        // ---

        // open button
        final double lat = getDouble(cursor, RatesPlacesTable.ColumnRatesPlaces.X);
        final double lng = getDouble(cursor, RatesPlacesTable.ColumnRatesPlaces.Y);

        final LatLng openPoint = new LatLng(lat, lng);

        ImageButton ibShowMap = holder.showMap;
        ibShowMap
                .setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        fragment.getTracker().trackPageView("/openMap");

                        Log.d(LOG_TAG, "open click " + openPoint);

                        fragment.getOpenListener().onOpen(openPoint);
                    }
                });

        //

        View.OnClickListener expandCollapseListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean expanded = whView.getVisibility() == View.VISIBLE;

                RotateAnimation rotate = new RotateAnimation(
                        expanded ? 90 : 0,
                        expanded ? 0 : 90,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setDuration(500);
                rotate.setFillEnabled(true);
                rotate.setFillAfter(true);
                ivExpandCollapse.startAnimation(rotate);

                shiftVisibility(whView);
                shiftVisibility(phonesView);
            }
        };

        ivExpandCollapse.setOnClickListener(expandCollapseListener);

        holder.expandCollapseArea.setOnClickListener(expandCollapseListener);

        final int regId = getInt(cursor, RatesPlacesTable.ColumnRatesPlaces.REGION_ID);
        final int currId = getInt(cursor, BestRatesTable.ColumnBestRates.CURRENCY_ID);
        final int exTypeId = getInt(cursor, BestRatesTable.ColumnBestRates.EXCHANGE_TYPE_ID);
        final double rate = getDouble(cursor, BestRatesTable.ColumnBestRates.VALUE);

        holder.showConverter
                .setOnClickListener(new android.view.View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        fragment.getTracker().trackPageView("/openConverter");

                        Master.OperationType operationType = Master.OperationType
                                .get(exTypeId);

                        Master.CurrencyCode currFrom = Master.CurrencyCode.get(currId);

                        Log.d(LOG_TAG, "converter click from: " + currFrom
                                + ", oper: " + operationType + ", rate: "
                                + rate);

                        ConverterFragment.ConverterParams converterParams = ConverterFragment.ConverterParams.instaniate(
                                Master.Region.get(regId), currFrom, operationType,
                                rate, fragment.getWorstRateValue());

                        fragment.getConverter().open(converterParams);
                    }
                });
    }

    private void fillRow(ViewHolder holder, Cursor cursor) {

        // format value
        fillText(holder.rateValue, cursor, BestRatesTable.ColumnBestRates.VALUE);
        fillText(holder.timeUpdated, cursor, BestRatesTable.ColumnBestRates.TIME_UPDATED);
        fillText(holder.description, cursor, RatesPlacesTable.ColumnRatesPlaces.DESCRIPTION);
        fillText(holder.address, cursor, RatesPlacesTable.ColumnRatesPlaces.ADDR);
        fillText(holder.workHours, cursor, RatesPlacesTable.ColumnRatesPlaces.WORK_HOURS);
        fillText(holder.phones, cursor, RatesPlacesTable.ColumnRatesPlaces.PHONE);

        // distance text
        Integer distance = fragment.getDistanceMap()[cursor.getPosition()];
        if (distance != null) {
            String txt = String.valueOf(distance) + " м";
            if (distance > 1000) {

                txt = DISTANCE_FORMAT.format(distance / 1000.0);
            }

            TextView tvDistance = holder.distance;
            tvDistance.setText(txt);
        }
    }

    private void fillText(TextView tv, Cursor cursor, DbColumn col) {
        tv.setText(cursor.getString(cursor.getColumnIndex(col.getName())));
    }

    private void shiftVisibility(View v) {

        if (v.getVisibility() == View.VISIBLE) {
            v.setVisibility(View.GONE);
        } else {
            v.setVisibility(View.VISIBLE);
        }
    }

    private int getInt(Cursor cursor, DbColumn col) {
        int index = cursor.getColumnIndex(col.getName());
        return cursor.getInt(index);
    }

    private double getDouble(Cursor cursor, DbColumn col) {
        int index = cursor.getColumnIndex(col.getName());
        return cursor.getDouble(index);
    }
}
