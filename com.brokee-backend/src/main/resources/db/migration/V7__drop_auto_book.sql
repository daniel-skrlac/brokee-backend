DECLARE
@sql nvarchar(max) = N'';

SELECT @sql = N'ALTER TABLE '
    + QUOTENAME(SCHEMA_NAME(t.schema_id)) + N'.' + QUOTENAME(t.name)
    + N' DROP CONSTRAINT ' + QUOTENAME(dc.name) + N';'
FROM sys.default_constraints dc
         JOIN sys.columns c
              ON c.object_id = dc.parent_object_id
                  AND c.column_id = dc.parent_column_id
         JOIN sys.tables t
              ON t.object_id = dc.parent_object_id
WHERE t.name = N'planned_tx'
  AND c.name = N'auto_book';

IF
@sql <> N'' EXEC sp_executesql @sql;

DECLARE
@idxsql nvarchar(max) = N'';

SELECT @idxsql =
       STRING_AGG(
               N'DROP INDEX ' + QUOTENAME(ix.name) + N' ON '
                   + QUOTENAME(SCHEMA_NAME(t.schema_id)) + N'.' + QUOTENAME(t.name) + N';',
           CHAR(10)
    )
FROM sys.indexes ix
         JOIN sys.index_columns ic
              ON ic.object_id = ix.object_id
                  AND ic.index_id = ix.index_id
         JOIN sys.columns c
              ON c.object_id = ic.object_id
                  AND c.column_id = ic.column_id
         JOIN sys.tables t
              ON t.object_id = ix.object_id
WHERE t.name = N'planned_tx'
  AND c.name = N'auto_book'
  AND ix.is_primary_key = 0
  AND ix.is_unique_constraint = 0;

IF
@idxsql IS NOT NULL AND @idxsql <> N'' EXEC sp_executesql @idxsql;

IF
COL_LENGTH('planned_tx','auto_book') IS NOT NULL
ALTER TABLE planned_tx DROP COLUMN auto_book;
