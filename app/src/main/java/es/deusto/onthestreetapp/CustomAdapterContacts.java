package es.deusto.onthestreetapp;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by kevin on 11/3/17.
 */

public class CustomAdapterContacts extends BaseAdapter implements View.OnClickListener{

    private ArrayList<Contact> filteredList;
    private CustomAdapterContacts current;
    private LayoutInflater inflater;
    Context mContext;

    private static class ViewHolderContacts {
        TextView txtNombre;
        TextView txtComentario;
        TextView txtPhone;
        ImageView icon;
    }

    public CustomAdapterContacts(ArrayList<Contact> data, Context context) {
        this.inflater = LayoutInflater.from(context);
        this.filteredList = data;
        this.mContext=context;

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        Place dataModel=(Place)object;

        switch (v.getId())
        {
            case R.id.item_info:
                MainActivity m = (MainActivity) mContext;
                ImageView preview = (ImageView) v.findViewById(R.id.item_info);
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
        Contact contacto = (Contact) getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolderContacts viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolderContacts();
            convertView = inflater.inflate(R.layout.row_contact, parent, false);
            viewHolder.txtNombre = (TextView) convertView.findViewById(R.id.nombre);
            viewHolder.txtComentario = (TextView) convertView.findViewById(R.id.direccion);
            viewHolder.txtPhone = (TextView) convertView.findViewById(R.id.Phone);
            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolderContacts) convertView.getTag();
            result=convertView;
        }
        viewHolder.txtNombre.setText(contacto.getName());
        viewHolder.txtComentario.setText(contacto.getDescription());
        viewHolder.txtPhone.setText("Phone: "+ contacto.getPhone());

        lastPosition = position;

        // Return the completed view to render on screen
        return convertView;
    }
}
