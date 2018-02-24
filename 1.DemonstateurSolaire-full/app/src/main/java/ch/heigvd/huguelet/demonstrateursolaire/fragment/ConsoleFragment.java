package ch.heigvd.huguelet.demonstrateursolaire.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import ch.heigvd.huguelet.demonstrateursolaire.MainActivity;
import ch.heigvd.huguelet.demonstrateursolaire.R;
import ch.heigvd.huguelet.demonstrateursolaire.utils.TextFormater;

public class ConsoleFragment extends Fragment {

    private Button btnSend;
    private EditText edtMessage;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;

    public ConsoleFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_console, container, false);

        messageListView = (ListView) view.findViewById(R.id.listMessage);
        btnSend=(Button) view.findViewById(R.id.sendButton);
        edtMessage = (EditText) view.findViewById(R.id.sendText);

        listAdapter = new ArrayAdapter<String>(getContext(), R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);

        // Handle Send button
        btnSend.setOnClickListener(v -> {

            String message = edtMessage.getText().toString();

            if (message != "") {

                ((MainActivity) getActivity()).sendDataOverBLE(message);
                edtMessage.setText("");
                write(TextFormater.getTimeFormated(), "TX", message);
            }

        });

        return view;
    }

    public void write(String timestamp, String type, String line){

        listAdapter.add( "[" + timestamp + "] " + type + (type == "" ? ": " : " : " ) + line );
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

    }

}
