package com.apps.ing3ns.entregas.Adaptadores;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.R;

import java.util.List;

/**
 * Created by julia on 14/06/2017.
 */

public class AdaptadorPedidos extends RecyclerView.Adapter<AdaptadorPedidos.ViewHolder>{

    private List<Delivery> deliveries;
    private Activity activity;
    private int layout;
    private OnItemClickListener itemClickListener;
    private Context context;

    public AdaptadorPedidos(List<Delivery> deliveries, int layout, Activity activity, OnItemClickListener listener){
        this.deliveries = deliveries;
        this.activity = activity;
        this.layout = layout;
        this.itemClickListener = listener;
    }

    public void setDeliveries(List<Delivery> deliveries){
        this.deliveries = deliveries;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(activity).inflate(layout, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(deliveries.get(position), itemClickListener);
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        animateCircularReveal(holder.itemView);
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder viewHolder) {
        super.onViewDetachedFromWindow(viewHolder);
        viewHolder.itemView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return deliveries.size();
    }

    public void animateCircularReveal(View view){
        int centerX = 0;
        int centerY = 0;
        int startRadius = 0;
        int endRadius = Math.max(view.getWidth(),view.getHeight());
        Animator animator = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            animator = ViewAnimationUtils.createCircularReveal(view,centerX,centerY,startRadius,endRadius);
            view.setVisibility(View.VISIBLE);
            animator.start();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView idText;
        public TextView addressStart;
        public TextView addressEnd;
        public TextView phone;
        public Button btnDeOne;
        public CardView cardView2;


        public ViewHolder(View itemView) {
            super(itemView);
            idText = itemView.findViewById(R.id.text_id_pedido);
            addressStart = itemView.findViewById(R.id.txt_addressStart);
            addressEnd = itemView.findViewById(R.id.txt_addressEnd);
            phone = itemView.findViewById(R.id.phone_pedido);
            cardView2 = itemView.findViewById(R.id.cardviewPedido);
            btnDeOne = itemView.findViewById(R.id.btnDone);
        }

        public void  bind(final Delivery delivery, final OnItemClickListener listener){

            idText.setText(String.valueOf(deliveries.indexOf(delivery)+1));
            addressStart.setText(delivery.getAddressStart());
            addressEnd.setText(delivery.getAddressEnd());
            phone.setText(delivery.getPhone());

            this.btnDeOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(delivery, getAdapterPosition());
                }

            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Delivery delivery, int position);
    }
}
