package com.taptrack.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.taptrack.app.data.local.entity.WaterMeterEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class WaterMeterDao_Impl implements WaterMeterDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WaterMeterEntity> __insertionAdapterOfWaterMeterEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteForTapStand;

  public WaterMeterDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWaterMeterEntity = new EntityInsertionAdapter<WaterMeterEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `water_meters` (`id`,`tapStandId`,`serialNumber`,`consumerName`,`readingDate`,`initialReading`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WaterMeterEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTapStandId());
        statement.bindString(3, entity.getSerialNumber());
        statement.bindString(4, entity.getConsumerName());
        statement.bindString(5, entity.getReadingDate());
        statement.bindDouble(6, entity.getInitialReading());
      }
    };
    this.__preparedStmtOfDeleteForTapStand = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM water_meters WHERE tapStandId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<WaterMeterEntity> meters,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWaterMeterEntity.insert(meters);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteForTapStand(final long tapStandId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteForTapStand.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, tapStandId);
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
          __preparedStmtOfDeleteForTapStand.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WaterMeterEntity>> getForTapStand(final long tapStandId) {
    final String _sql = "SELECT * FROM water_meters WHERE tapStandId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, tapStandId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"water_meters"}, new Callable<List<WaterMeterEntity>>() {
      @Override
      @NonNull
      public List<WaterMeterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTapStandId = CursorUtil.getColumnIndexOrThrow(_cursor, "tapStandId");
          final int _cursorIndexOfSerialNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "serialNumber");
          final int _cursorIndexOfConsumerName = CursorUtil.getColumnIndexOrThrow(_cursor, "consumerName");
          final int _cursorIndexOfReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "readingDate");
          final int _cursorIndexOfInitialReading = CursorUtil.getColumnIndexOrThrow(_cursor, "initialReading");
          final List<WaterMeterEntity> _result = new ArrayList<WaterMeterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WaterMeterEntity _item;
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
            _item = new WaterMeterEntity(_tmpId,_tmpTapStandId,_tmpSerialNumber,_tmpConsumerName,_tmpReadingDate,_tmpInitialReading);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
