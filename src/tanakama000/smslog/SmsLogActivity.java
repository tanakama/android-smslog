package tanakama000.smslog;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsLogActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// 通信ログ取得
		// SMS
		ArrayList<HashMap<String, Object>> smsList = getSmsLog();
		// ContactMap
		ContactMap contactMap = new ContactMap();
		//
		ArrayList<HashMap<String, Object>> adapterList = new ArrayList<HashMap<String,Object>>();

		for ( HashMap<String, Object>item:smsList )
		{
			HashMap<String, Object>adapterItem = new HashMap<String, Object>();
			HashMap<String, Object>contactItem = contactMap.getItem((String) item.get("address"));
			String[] inOutText = {"From: ", "To: ", "NG: " };
			int inOutIndex = Integer.parseInt(((String)item.get("type")));
			if (contactItem.isEmpty())
			{
				adapterItem.put("nameNumbertext", inOutText[inOutIndex-1] + item.get("address"));
			}
			else
			{
				adapterItem.put("nameNumbertext", inOutText[inOutIndex-1] + contactItem.get("display_name") + "(" + item.get("address") + ")");
				adapterItem.put("photo", contactItem.get("photo"));
			}
			adapterItem.put("dateDuarationtext", item.get("date"));
			adapterItem.put("messagetext", item.get("body"));
			adapterItem.put("inOutType", item.get("type"));
			adapterList.add(adapterItem);
		}

		SmsLogRecordAdapter adapter = new SmsLogRecordAdapter(this, adapterList, R.layout.smslogrecord,
				new String[]{"dateDuarationtext","nameNumbertext","messagetext"},
				new int[]{R.id.dateDuarationtext, R.id.nameNumbertext, R.id.messagetext});

		ListView lv = (ListView) findViewById(R.id.listview);
		lv.setAdapter(adapter);
	}

	private ArrayList<HashMap<String, Object>> getSmsLog()
	{
		// 日付フォーマット定義
		SimpleDateFormat dateForm = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		// 取得データ定義
		String[] projection = {"_id", "thread_id", "date", "type", "address", "subject", "body"};
		// カーソル定義
		Cursor cur = getContentResolver().query(Uri.parse("content://sms/"), projection, null, null, "date DESC");
		// 返却用ArrayList定義
		ArrayList<HashMap<String,Object>> arrayList = new ArrayList<HashMap<String,Object>>();

		while (cur.moveToNext())
		{
			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			for (String keyName : projection)
			{
				if (keyName.equals("date"))
				{
					hashMap.put(keyName, dateForm.format(new Date(cur.getLong(cur.getColumnIndex("date")))));
				}
				else
				{
					hashMap.put(keyName, cur.getString(cur.getColumnIndex(keyName)));
				}
			}
			arrayList.add(hashMap);
		}
		cur.close();
		return arrayList;
	}

	private class ContactMap extends HashMap<String,HashMap<String,Object>>
	{
		private static final long serialVersionUID = 1L;

		public ContactMap()
		{
			super();
		}

		public HashMap<String,Object> getItem(String number)
		{
			if (! this.containsKey(number))
			{
				HashMap<String,Object> contactItem = getContact(number);
				this.put(number, contactItem);


			}
			return this.get(number);
		}

		private HashMap<String, Object> getContact(String number)
		{
			Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.decode(number));
			Cursor cur = getContentResolver().query(uri, null, null, null, null);
			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			if (cur.moveToFirst())
			{
				for(String keyName: cur.getColumnNames())
				{
					hashMap.put(keyName, cur.getString(cur.getColumnIndex(keyName)));
				}
				hashMap.put("photo", getPhotoThumbnail(cur.getLong(cur.getColumnIndex("_id"))));
			}
			cur.close();
			return hashMap;
		}

		private Bitmap getPhotoThumbnail(long contactId)
		{
			Uri uri =  Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
					ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
			Cursor cur = getContentResolver().query(uri, new String[] {ContactsContract.Contacts.Photo.DATA15}, null, null, null);
			try {
				if (cur.moveToFirst())
				{
					byte[] data = cur.getBlob(cur.getColumnIndex(ContactsContract.Contacts.Photo.DATA15));
					if (data != null)
					{
						return new BitmapDrawable(new ByteArrayInputStream(data)).getBitmap();

					}
				}
			}
			finally
			{
				cur.close();
			}
			return null;
		}

	}
	public class SmsLogRecordAdapter extends SimpleAdapter
	{
		LayoutInflater mLayoutInflater;

		public SmsLogRecordAdapter( Context context, List<? extends Map<String, ?>> data,
				int resource, String[] from, int[] to) {
			super( context, data, resource, from, to);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			mLayoutInflater = LayoutInflater.from(getBaseContext());
			convertView = mLayoutInflater.inflate(R.layout.smslogrecord, parent, false);
			ListView listView = (ListView)parent;

			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>)listView.getItemAtPosition(position);

			if (data.get("photo") != null)
			{
				ImageView contactImageView = (ImageView)convertView.findViewById(R.id.contactImage);
				contactImageView.setImageBitmap((Bitmap)data.get("photo"));
			}

			ImageView statusImageView = (ImageView)convertView.findViewById(R.id.statusImage);
			switch (Integer.parseInt(((String)data.get("inOutType"))))
			{
			case 1:
				statusImageView.setImageResource(R.drawable.incoming_icon);
				break;
			case 2:
				statusImageView.setImageResource(R.drawable.outgoing_icon);
				break;
			default:
				statusImageView.setImageResource(R.drawable.outgingng_icon);
			}

			TextView dateDuarationtextView = (TextView)convertView.findViewById(R.id.dateDuarationtext);
			dateDuarationtextView.setText((String)data.get("dateDuarationtext"));

			TextView nameNumbertextView = (TextView)convertView.findViewById(R.id.nameNumbertext);
			nameNumbertextView.setText((String)data.get("nameNumbertext"));

			TextView messagetextView = (TextView)convertView.findViewById(R.id.messagetext);
			messagetextView.setText((String)data.get("messagetext"));

			return convertView;
		}
	}
}