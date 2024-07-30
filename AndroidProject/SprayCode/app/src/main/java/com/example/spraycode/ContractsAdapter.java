package com.example.spraycode;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class ContractsAdapter extends RecyclerView.Adapter<ContractsAdapter.ViewHolder> {

    private static List<Contract> contracts;

    public ContractsAdapter(List<Contract> contracts) {
        ContractsAdapter.contracts = contracts;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView contractName;
        public TextView contractCosting;
        public TextView contractExpiry;
        public TextView contractorName;
        public TextView contractStatus;

        public ViewHolder(View view) {
            super(view);
            contractName = view.findViewById(R.id.contract_name);
            contractCosting = view.findViewById(R.id.contract_costing);
            contractExpiry = view.findViewById(R.id.contract_expiry);
            contractorName = view.findViewById(R.id.contractor_name);
            contractStatus = view.findViewById(R.id.contract_status);
            view.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Toast.makeText(view.getContext(), "Clicked: " + contractName.getText(), Toast.LENGTH_SHORT).show();
                            Toast.makeText(view.getContext(), "Role: " + ContractsAdapter.contracts.get(position).getRole(), Toast.LENGTH_SHORT).show();
                            if (ContractsAdapter.contracts.get(position).getRole().equals("publisher")) {
                                Intent intent = new Intent(view.getContext(), EmployerViewContract.class);
                                intent.putExtra("contract_id", contractName.getText());
                                view.getContext().startActivity(intent);
                            } else if (ContractsAdapter.contracts.get(position).getRole().equals("contractor")) {
                                Intent intent = new Intent(view.getContext(), EmployeeViewContract.class);
                                intent.putExtra("contract_id", contractName.getText());
                                view.getContext().startActivity(intent);
                            }
                        }
                    }
                }
            );
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contract_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contract contract = contracts.get(position);
        holder.contractName.setText(contract.getContractName());
        holder.contractorName.setText(contract.getContractorName());
        holder.contractCosting.setText("Costing: $" + contract.getCosting());
        holder.contractExpiry.setText("Expiry: " + contract.getExpiry());
        holder.contractStatus.setText(contract.getStatus());
    }

    @Override
    public int getItemCount() {
        return contracts.size();
    }
}