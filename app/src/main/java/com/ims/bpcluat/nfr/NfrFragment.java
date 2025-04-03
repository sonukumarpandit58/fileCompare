package com.ims.bpcluat.nfr;

import static android.content.Context.MODE_PRIVATE;

import static com.ims.bpcluat.Helper.fuelProductList;
import static com.ims.bpcluat.Helper.getProductId;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.ims.bpcluat.R;
import com.ims.bpcluat.databinding.FragmentNfrBinding;

public class NfrFragment extends Fragment {

    FragmentNfrBinding binding;
   // String[] nfrCategoryListArray = {"2 T","4 T","AGRI OILS","AUTO GAS","AUTO GEAR","CVO","DEF","GREASE","OEM","PCMO","PCVO","SPECIALTY","TRANS OIL"};
    String[] nfrCategoryListArray = {"2T Oil","DEF","GREASE","Coolant","Engine Oil"};

    public NfrFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("cngProduct1",getProductId("LUBES",fuelProductList));
        SharedPreferences shared =  getActivity().getSharedPreferences("nfrSharedPreferencesData", MODE_PRIVATE);
        String nfrProductName = (shared.getString("nfrProductName", ""));
        String nfrProductQty = (shared.getString("nfrProductQty", ""));
        String nfrProductAmt = (shared.getString("nfrProductAmt", ""));
        if (!TextUtils.isEmpty(nfrProductName) && !TextUtils.isEmpty(nfrProductQty) && !TextUtils.isEmpty(nfrProductAmt)) {
             setHasOptionsMenu(true); // Important to enable options menu in the fragment
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNfrBinding.inflate(inflater, container, false);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, nfrCategoryListArray);
        binding.nfrCategory.setAdapter(adapter);

        binding.nfrCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(getActivity(), NfrProductAddActivity.class);
                intent.putExtra("productName", selectedItem);
                startActivity(intent);
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_cart, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.cart) {
            Intent intent = new Intent(getActivity(), NfrCartActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
