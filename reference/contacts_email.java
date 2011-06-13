int idx = Utils.getContactsIDbyPhonenumber(_phonenumber);
  if(idx == -1) return null;
  
  Uri mUri = ContentUris.withAppendedId(People.CONTENT_URI, idx );
        Uri uri = Uri.withAppendedPath(mUri, Contacts.People.ContactMethods.CONTENT_DIRECTORY);
          
        String selection = Contacts.ContactMethodsColumns.KIND + "=?";
        String value = String.valueOf(Contacts.KIND_EMAIL);
        String[] args = new String[] {value};
        
      ContentResolver resolver = mMain.getContentResolver();
      Cursor cursor = resolver.query(uri, null, selection, args, null);
      int rc = cursor.getCount();
      if(cursor.moveToFirst())  {
      // �÷������� �÷� �ε��� ã��
       int iId = cursor.getColumnIndex(Contacts.ContactMethods._ID);
          int iType = cursor.getColumnIndex(Contacts.ContactMethods.TYPE);
          int iData = cursor.getColumnIndex(Contacts.ContactMethods.DATA);
           // ��Ұ� ���
          int id = cursor.getInt(iId);
          int type = cursor.getInt(iType);
          String email = cursor.getString(iData);
          return email;
        }