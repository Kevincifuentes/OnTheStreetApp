package es.deusto.onthestreetapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by kevin on 11/3/17.
 */

public class CustomAdapter extends BaseAdapter implements View.OnClickListener, Filterable{

    private ArrayList<Place> dataSet;
    private ArrayList<Place> filteredList;
    private PlaceFilter placeFilter;
    private CustomAdapter current;
    private LayoutInflater inflater;
    Context mContext;

    private static class ViewHolder {
        TextView txtNombre;
        TextView txtDireccion;
        TextView distance;
        ImageView icon;
    }

    public CustomAdapter(ArrayList<Place> data, Context context) {
        this.dataSet = data;
        this.inflater = LayoutInflater.from(context);
        this.filteredList = data;
        this.mContext=context;

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        Place dataModel=(Place)object;
        Log.i("click","");
        switch (v.getId())
        {
            case R.id.item_info:
                MainActivity m = (MainActivity) mContext;
                ImageView preview = new ImageView(mContext);
                if(dataModel.getImageURI() != null){
                    preview.setImageURI(Uri.parse(dataModel.getImageURI()));
                }
                else{
                    preview.setImageResource(R.drawable.example);
                }
                m.showImage(preview);
                break;
        }
    }

    private int lastPosition = -1;

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Place place = (Place) getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.row_item, parent, false);
            viewHolder.txtNombre = (TextView) convertView.findViewById(R.id.nombre);
            viewHolder.txtDireccion = (TextView) convertView.findViewById(R.id.direccion);
            viewHolder.distance = (TextView) convertView.findViewById(R.id.label_metros);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.item_info);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        //Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        //result.startAnimation(animation);
        lastPosition = position;

        viewHolder.txtNombre.setText(place.getNombre());
        viewHolder.txtDireccion.setText(place.getDireccion());
        if(place.getImageURI() != null){
            File f = new File(place.getImageURI());
            Picasso.with(mContext)
                    .load(f)
                    .resize(50, 50)
                    .centerCrop()
                    .into(viewHolder.icon);
        }
        else{
            Picasso.with(mContext)
                    .load(R.drawable.example)
                    .resize(50, 50)
                    .centerCrop()
                    .into(viewHolder.icon);
        }

        viewHolder.icon.setOnClickListener(this);
        viewHolder.icon.setTag(position);
        convertView.setBackgroundColor(Color.rgb(255,255,255));
        if(MainActivity.longClickedPos != -1){
            if(MainActivity.longClickedPos == position){
                convertView.setBackgroundColor(Color.rgb(118, 172, 0));
            }
        }

        if(MainActivity.checked){
            viewHolder.distance.setText(mContext.getString(R.string.distance)+ " "+ String.format("%.2f", place.getComparedDistance())+ " "+ mContext.getString(R.string.meters));
            viewHolder.distance.setVisibility(View.VISIBLE);
        }
        else{
            viewHolder.distance.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

    /**
     * Get custom filter
     * @return filter
     */
    @Override
    public Filter getFilter() {
        if (placeFilter == null) {
            placeFilter = new PlaceFilter();
        }

        return placeFilter;
    }

    private class PlaceFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            Log.i("Filtrando...", "Fuera");
            Log.i("Por", constraint.toString());
            if (constraint!=null && constraint.length()>0) {
                Log.i("Filtrando...", "1");
                ArrayList<Place> tempList = new ArrayList<Place>();

                // search content in friend list
                for (Place place : dataSet) {
                    if(place.getDesc().toLowerCase().contains(constraint.toString().toLowerCase())
                            || place.getDireccion().toLowerCase().contains(constraint.toString().toLowerCase())
                            || place.getNombre().toLowerCase().contains(constraint.toString().toLowerCase())
                            || (place.getLat()+"").toLowerCase().contains(constraint.toString().toLowerCase()) ||
                            (place.getLng()+"").toLowerCase().contains(constraint.toString().toLowerCase()) )
                    {
                        tempList.add(place);
                    }
                }

                filterResults.count = tempList.size();
                filterResults.values = tempList;
            } else {
                Log.i("Filtrando...", "2");
                filterResults.count = dataSet.size();
                filterResults.values = dataSet;
            }
            Log.i("Antes de salir---", filterResults.count+"");

            return filterResults;
        }

        /**
         * Notify about filtered list to ui
         * @param constraint text
         * @param results filtered result
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Log.i("Número", ((ArrayList<Place>) results.values).size()+"");
            filteredList = (ArrayList<Place>) results.values;
            notifyDataSetChanged();

        }
    }
}
