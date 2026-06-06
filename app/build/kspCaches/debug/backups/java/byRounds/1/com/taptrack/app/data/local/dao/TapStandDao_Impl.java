package com.taptrack.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.taptrack.app.data.local.entity.TapStandEntity;
import com.taptrack.app.data.local.entity.WaterMeterEntity;
import com.taptrack.app.data.model.TapStandWithMeters;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TapStandDao_Impl implements TapStandDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TapStandEntity> __insertionAdapterOfTapStandEntity;

  private final EntityDeletionOrUpdateAdapter<TapStandEntity> __updateAdapterOfTapStandEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public TapStandDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTapStandEntity = new EntityInsertionAdapter<TapStandEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tap_stands` (`id`,`name`,`locationDescription`,`latitude`,`longitude`,`photoPath`,`installationDate`,`status`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TapStandEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getLocationDescription());
        statement.bindDouble(4, entity.getLatitude());
        statement.bindDouble(5, entity.getLongitude());
        statement.bindString(6, entity.getPhotoPath());
        statement.bindString(7, entity.getInstallationDate());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__updateAdapterOfTapStandEntity = new EntityDeletionOrUpdateAdapter<TapStandEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tap_stands` SET `id` = ?,`name` = ?,`locationDescription` = ?,`latitude` = ?,`longitude` = ?,`photoPath` = ?,`installationDate` = ?,`status` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TapStandEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getLocationDescription());
        statement.bindDouble(4, entity.getLatitude());
        statement.bindDouble(5, entity.getLongitude());
        statement.bindString(6, entity.getPhotoPath());
        statement.bindString(7, entity.getInstallationDate());
        statement.bindString(8, entity.getStatus());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tap_stands WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TapStandEntity tapStand,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTapStandEntity.insertAndReturnId(tapStand);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final TapStandEntity tapStand,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTapStandEntity.handle(tapStand);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TapStandWithMeters>> getAllWithMeters() {
    final String _sql = "SELECT * FROM tap_stands ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"water_meters",
        "tap_stands"}, new Callable<List<TapStandWithMeters>>() {
      @Override
      @NonNull
      public List<TapStandWithMeters> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLocationDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "locationDescription");
            final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
            final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
            final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
            final int _cursorIndexOfInstallationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "installationDate");
            final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final LongSparseArray<ArrayList<WaterMeterEntity>> _collectionMeters = new LongSparseArray<ArrayList<WaterMeterEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionMeters.containsKey(_tmpKey)) {
                _collectionMeters.put(_tmpKey, new ArrayList<WaterMeterEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipwaterMetersAscomTaptrackAppDataLocalEntityWaterMeterEntity(_collectionMeters);
            final List<TapStandWithMeters> _result = new ArrayList<TapStandWithMeters>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TapStandWithMeters _item;
              final TapStandEntity _tmpTapStand;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final String _tmpLocationDescription;
              _tmpLocationDescription = _cursor.getString(_cursorIndexOfLocationDescription);
              final double _tmpLatitude;
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
              final double _tmpLongitude;
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
              final String _tmpPhotoPath;
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
              final String _tmpInstallationDate;
              _tmpInstallationDate = _cursor.getString(_cursorIndexOfInstallationDate);
              final String _tmpStatus;
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              _tmpTapStand = new TapStandEntity(_tmpId,_tmpName,_tmpLocationDescription,_tmpLatitude,_tmpLongitude,_tmpPhotoPath,_tmpInstallationDate,_tmpStatus,_tmpCreatedAt);
              final ArrayList<WaterMeterEntity> _tmpMetersCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpMetersCollection = _collectionMeters.get(_tmpKey_1);
              _item = new TapStandWithMeters(_tmpTapStand,_tmpMetersCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id, final Continuation<? super TapStandEntity> $completion) {
    final String _sql = "SELECT * FROM tap_stands WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TapStandEntity>() {
      @Override
      @Nullable
      public TapStandEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLocationDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "locationDescription");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfInstallationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "installationDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final TapStandEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpLocationDescription;
            _tmpLocationDescription = _cursor.getString(_cursorIndexOfLocationDescription);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpPhotoPath;
            _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            final String _tmpInstallationDate;
            _tmpInstallationDate = _cursor.getString(_cursorIndexOfInstallationDate);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new TapStandEntity(_tmpId,_tmpName,_tmpLocationDescription,_tmpLatitude,_tmpLongitude,_tmpPhotoPath,_tmpInstallationDate,_tmpStatus,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getWithMetersById(final long id,
      final Continuation<? super TapStandWithMeters> $completion) {
    final String _sql = "SELECT * FROM tap_stands WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<TapStandWithMeters>() {
      @Override
      @Nullable
      public TapStandWithMeters call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLocationDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "locationDescription");
            final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
            final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
            final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
            final int _cursorIndexOfInstallationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "installationDate");
            final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final LongSparseArray<ArrayList<WaterMeterEntity>> _collectionMeters = new LongSparseArray<ArrayList<WaterMeterEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionMeters.containsKey(_tmpKey)) {
                _collectionMeters.put(_tmpKey, new ArrayList<WaterMeterEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipwaterMetersAscomTaptrackAppDataLocalEntityWaterMeterEntity(_collectionMeters);
            final TapStandWithMeters _result;
            if (_cursor.moveToFirst()) {
              final TapStandEntity _tmpTapStand;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final String _tmpLocationDescription;
              _tmpLocationDescription = _cursor.getString(_cursorIndexOfLocationDescription);
              final double _tmpLatitude;
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
              final double _tmpLongitude;
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
              final String _tmpPhotoPath;
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
              final String _tmpInstallationDate;
              _tmpInstallationDate = _cursor.getString(_cursorIndexOfInstallationDate);
              final String _tmpStatus;
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              _tmpTapStand = new TapStandEntity(_tmpId,_tmpName,_tmpLocationDescription,_tmpLatitude,_tmpLongitude,_tmpPhotoPath,_tmpInstallationDate,_tmpStatus,_tmpCreatedAt);
              final ArrayList<WaterMeterEntity> _tmpMetersCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpMetersCollection = _collectionMeters.get(_tmpKey_1);
              _result = new TapStandWithMeters(_tmpTapStand,_tmpMetersCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshipwaterMetersAscomTaptrackAppDataLocalEntityWaterMeterEntity(
      @NonNull final LongSparseArray<ArrayList<WaterMeterEntity>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshipwaterMetersAscomTaptrackAppDataLocalEntityWaterMeterEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`tapStandId`,`serialNumber`,`consumerName`,`readingDate`,`initialReading` FROM `water_meters` WHERE `tapStandId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "tapStandId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfTapStandId = 1;
      final int _cursorIndexOfSerialNumber = 2;
      final int _cursorIndexOfConsumerName = 3;
      final int _cursorIndexOfReadingDate = 4;
      final int _cursorIndexOfInitialReading = 5;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<WaterMeterEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final WaterMeterEntity _item_1;
          final long _tmpId;
          _tmpId = _cursor.getLong(_cursorIndexOfId);
          final long _tmpTapStandId;
          _tmpTapStandId = _cursor.getLong(_cursorIndexOfTapStandId);
          final String _tmpSerialNumber;
          _tmpSerialNumber = _cursor.getString(_cursorIndexOfSerialNumber);
          final String _tmpConsumerName;
          _tmpConsumerName = _cursor.getString(_cursorIndexOfConsumerName);
          final String _tmpReadingDate;
          _tmpReadingDate = _cursor.getString(_cursorIndexOfReadingDate);
          final double _tmpInitialReading;
          _tmpInitialReading = _cursor.getDouble(_cursorIndexOfInitialReading);
          _item_1 = new WaterMeterEntity(_tmpId,_tmpTapStandId,_tmpSerialNumber,_tmpConsumerName,_tmpReadingDate,_tmpInitialReading);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
