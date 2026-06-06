package com.taptrack.app.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.taptrack.app.data.local.dao.TapStandDao;
import com.taptrack.app.data.local.dao.TapStandDao_Impl;
import com.taptrack.app.data.local.dao.WaterMeterDao;
import com.taptrack.app.data.local.dao.WaterMeterDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile TapStandDao _tapStandDao;

  private volatile WaterMeterDao _waterMeterDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tap_stands` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `locationDescription` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `photoPath` TEXT NOT NULL, `installationDate` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `water_meters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tapStandId` INTEGER NOT NULL, `serialNumber` TEXT NOT NULL, `consumerName` TEXT NOT NULL, `readingDate` TEXT NOT NULL, `initialReading` REAL NOT NULL, FOREIGN KEY(`tapStandId`) REFERENCES `tap_stands`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_meters_tapStandId` ON `water_meters` (`tapStandId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3f3ad71e31cf7b6e4a5034da9ec733b2')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `tap_stands`");
        db.execSQL("DROP TABLE IF EXISTS `water_meters`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTapStands = new HashMap<String, TableInfo.Column>(9);
        _columnsTapStands.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("locationDescription", new TableInfo.Column("locationDescription", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("photoPath", new TableInfo.Column("photoPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("installationDate", new TableInfo.Column("installationDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTapStands.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTapStands = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTapStands = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTapStands = new TableInfo("tap_stands", _columnsTapStands, _foreignKeysTapStands, _indicesTapStands);
        final TableInfo _existingTapStands = TableInfo.read(db, "tap_stands");
        if (!_infoTapStands.equals(_existingTapStands)) {
          return new RoomOpenHelper.ValidationResult(false, "tap_stands(com.taptrack.app.data.local.entity.TapStandEntity).\n"
                  + " Expected:\n" + _infoTapStands + "\n"
                  + " Found:\n" + _existingTapStands);
        }
        final HashMap<String, TableInfo.Column> _columnsWaterMeters = new HashMap<String, TableInfo.Column>(6);
        _columnsWaterMeters.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWaterMeters.put("tapStandId", new TableInfo.Column("tapStandId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWaterMeters.put("serialNumber", new TableInfo.Column("serialNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWaterMeters.put("consumerName", new TableInfo.Column("consumerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWaterMeters.put("readingDate", new TableInfo.Column("readingDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWaterMeters.put("initialReading", new TableInfo.Column("initialReading", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWaterMeters = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysWaterMeters.add(new TableInfo.ForeignKey("tap_stands", "CASCADE", "NO ACTION", Arrays.asList("tapStandId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesWaterMeters = new HashSet<TableInfo.Index>(1);
        _indicesWaterMeters.add(new TableInfo.Index("index_water_meters_tapStandId", false, Arrays.asList("tapStandId"), Arrays.asList("ASC")));
        final TableInfo _infoWaterMeters = new TableInfo("water_meters", _columnsWaterMeters, _foreignKeysWaterMeters, _indicesWaterMeters);
        final TableInfo _existingWaterMeters = TableInfo.read(db, "water_meters");
        if (!_infoWaterMeters.equals(_existingWaterMeters)) {
          return new RoomOpenHelper.ValidationResult(false, "water_meters(com.taptrack.app.data.local.entity.WaterMeterEntity).\n"
                  + " Expected:\n" + _infoWaterMeters + "\n"
                  + " Found:\n" + _existingWaterMeters);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3f3ad71e31cf7b6e4a5034da9ec733b2", "2706d590092c3668b6694a55aeeea40e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "tap_stands","water_meters");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `tap_stands`");
      _db.execSQL("DELETE FROM `water_meters`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TapStandDao.class, TapStandDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WaterMeterDao.class, WaterMeterDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TapStandDao tapStandDao() {
    if (_tapStandDao != null) {
      return _tapStandDao;
    } else {
      synchronized(this) {
        if(_tapStandDao == null) {
          _tapStandDao = new TapStandDao_Impl(this);
        }
        return _tapStandDao;
      }
    }
  }

  @Override
  public WaterMeterDao waterMeterDao() {
    if (_waterMeterDao != null) {
      return _waterMeterDao;
    } else {
      synchronized(this) {
        if(_waterMeterDao == null) {
          _waterMeterDao = new WaterMeterDao_Impl(this);
        }
        return _waterMeterDao;
      }
    }
  }
}
