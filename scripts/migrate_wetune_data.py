#!/usr/bin/env python3
"""
从 WeTune 数据导出 schema DDL 和 SQL 查询到 AutoRewriter 测试资源目录。

用法:
    python3 scripts/migrate_wetune_data.py \
        --wetune-root /mnt/mydata/yyk/wetune \
        --output-root /mnt/mydata/yyk/AutoRewriter/test/src/main/resources/schema \
        [--apps diaspora discourse] \
        [--min-tables 2]  # 过滤单表查询
"""

import argparse
import sqlite3
import re
import sys
from pathlib import Path

# PostgreSQL 方言的 app（DDL 已是 PostgreSQL 格式，只需加 schema 前缀）
POSTGRES_APPS = {"discourse", "gitlab", "homeland"}


def is_simple_query(sql: str) -> bool:
    """过滤单表简单查询（无 JOIN、无子查询）"""
    sql_upper = sql.upper()
    if "JOIN" in sql_upper:
        return False
    if sql_upper.count("SELECT") > 1:
        return False
    return True


def convert_mysql_ddl_to_pg(ddl: str, schema_name: str) -> str:
    """
    将 MySQL DDL 转换为 PostgreSQL 格式：
    - 去掉 MySQL 特有指令（/*!...*/, SET, LOCK, DROP TABLE）
    - CREATE TABLE `name` → CREATE TABLE IF NOT EXISTS schema.name
    - 去掉 AUTO_INCREMENT, CHARACTER SET, COLLATE, COMMENT
    - 类型转换：tinyint(1)→boolean, tinyint→smallint, int(N)→integer, bigint(N)→bigint, datetime→timestamp
    - 去掉 KEY/INDEX 行（非 PRIMARY KEY）
    - 去掉 CONSTRAINT FOREIGN KEY 行（PostgreSQL 不需要，且引用表可能不存在）
    - 去掉 UNIQUE KEY 行（简化，避免依赖问题）
    - 去掉 ); 前的尾随逗号
    - varchar(N) 保持不变（PostgreSQL 支持）
    - mediumtext/longtext/text → text
    - mediumint → integer
    - float/double → double precision
    - json → jsonb
    """
    lines = []
    in_create_table = False

    for line in ddl.splitlines():
        stripped = line.strip()

        # 跳过 MySQL 特有指令
        if (stripped.startswith('/*!') or stripped.startswith('SET ') or
                stripped.startswith('LOCK ') or stripped.startswith('UNLOCK ') or
                stripped.startswith('--') or
                re.match(r'DROP TABLE', stripped, re.IGNORECASE)):
            continue

        if stripped == '':
            lines.append('')
            continue

        # CREATE TABLE
        m = re.match(r'CREATE TABLE\s+`([^`]+)`\s*\(', stripped, re.IGNORECASE)
        if m:
            table_name = m.group(1)
            lines.append(f'CREATE TABLE IF NOT EXISTS {schema_name}."{table_name}" (')
            in_create_table = True
            continue

        if in_create_table:
            # 结束行：) ENGINE=... 或 );
            if re.match(r'\)\s*(ENGINE|DEFAULT CHARSET|AUTO_INCREMENT|ROW_FORMAT|COMMENT|;)', stripped, re.IGNORECASE) or stripped == ')':
                lines.append(');')
                in_create_table = False
                continue

            # 去掉 backtick
            col_line = line.replace('`', '')

            # 跳过 KEY / INDEX / UNIQUE KEY / CONSTRAINT FOREIGN KEY 行
            if re.match(r'\s*(KEY|INDEX|UNIQUE KEY|UNIQUE INDEX|CONSTRAINT\s+\S+\s+FOREIGN KEY|FOREIGN KEY)\s*', col_line, re.IGNORECASE):
                continue

            # 类型转换
            col_line = re.sub(r'\btinyint\s*\(\s*1\s*\)', 'boolean', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\btinyint\b', 'smallint', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bmediumint\s*\(\s*\d+\s*\)', 'integer', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bmediumint\b', 'integer', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bint\s*\(\s*\d+\s*\)', 'integer', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bbigint\s*\(\s*\d+\s*\)', 'bigint', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bdatetime\b', 'timestamp', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\b(mediumtext|longtext)\b', 'text', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bdouble\b', 'double precision', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bjson\b', 'jsonb', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bblob\b', 'bytea', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\bmediumblob\b', 'bytea', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\blongblob\b', 'bytea', col_line, flags=re.IGNORECASE)

            # 去掉 MySQL 列选项
            col_line = re.sub(r'\s+AUTO_INCREMENT', '', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\s+CHARACTER SET\s+\S+', '', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\s+COLLATE\s+\S+', '', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r"\s+COMMENT\s+'[^']*'", '', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\s+ON UPDATE\s+\S+', '', col_line, flags=re.IGNORECASE)
            col_line = re.sub(r'\s+UNSIGNED\b', '', col_line, flags=re.IGNORECASE)

            # 给列名加双引号，避免与 PostgreSQL 保留字冲突
            # 只处理普通列定义行（不处理 PRIMARY KEY / CONSTRAINT 等）
            if not re.match(r'\s*(PRIMARY\s+KEY|CONSTRAINT|UNIQUE\s+KEY|UNIQUE\s+INDEX)\b', col_line, re.IGNORECASE):
                col_line = re.sub(
                    r'^(\s+)([a-zA-Z_][a-zA-Z0-9_]*)(\s)',
                    lambda m: m.group(1) + '"' + m.group(2) + '"' + m.group(3),
                    col_line
                )

            lines.append(col_line)
        else:
            lines.append(line)

    # 后处理：
    # 1. 去掉 ); 前的尾随逗号（PRIMARY KEY 行末尾的逗号）
    # 2. 确保列定义行之间有逗号（删除 KEY/INDEX 行后可能丢失逗号）
    result_lines = []
    for i, line in enumerate(lines):
        if line.strip() == ');' and result_lines:
            prev = result_lines[-1].rstrip()
            if prev.endswith(','):
                result_lines[-1] = prev[:-1]
        result_lines.append(line)

    # 第二遍：在 CREATE TABLE 内部，如果某行不以逗号结尾，
    # 且下一个非空行也是列定义（不是 );），则补上逗号
    final_lines = []
    in_ct = False
    for i, line in enumerate(result_lines):
        s = line.strip()
        if re.match(r'CREATE TABLE', s, re.IGNORECASE):
            in_ct = True
        if s == ');':
            in_ct = False
        if in_ct and s and not s.startswith('--'):
            # 找下一个非空行
            j = i + 1
            while j < len(result_lines) and result_lines[j].strip() == '':
                j += 1
            next_s = result_lines[j].strip() if j < len(result_lines) else ''
            # 如果当前行不以逗号/( 结尾，且下一行不是 ); 也不是空
            if (next_s and next_s != ');' and
                    not line.rstrip().endswith(',') and
                    not line.rstrip().endswith('(') and
                    not s.startswith('CREATE TABLE')):
                line = line.rstrip() + ','
        final_lines.append(line)

    return '\n'.join(final_lines)


def export_schema(wetune_root: Path, app: str, output_dir: Path):
    """复制并转换 schema DDL 文件"""
    schema_file = wetune_root / "wtune_data" / "schemas" / f"{app}.base.schema.sql"
    if not schema_file.exists():
        print(f"  [WARN] Schema not found: {schema_file}")
        return False

    output_dir.mkdir(parents=True, exist_ok=True)
    dest = output_dir / "table_ddl.sql"

    raw_ddl = schema_file.read_text(encoding="utf-8", errors="replace")

    if app in POSTGRES_APPS:
        # PostgreSQL DDL：已有 public.table 格式，替换 schema 名
        converted = raw_ddl.replace('public.', f'{app}.')
        # 去掉 CREATE SCHEMA / EXTENSION / SET 等非 CREATE TABLE 语句
        lines = []
        for line in converted.splitlines():
            s = line.strip().upper()
            if s.startswith('CREATE SCHEMA') or s.startswith('CREATE EXTENSION') or \
               s.startswith('SET ') or s.startswith('SELECT PG_CATALOG') or \
               s.startswith('ALTER ') or s.startswith('GRANT ') or \
               s.startswith('REVOKE ') or s.startswith('COMMENT ON'):
                continue
            lines.append(line)
        converted = '\n'.join(lines)
    else:
        # MySQL DDL：转换为 PostgreSQL 格式
        converted = convert_mysql_ddl_to_pg(raw_ddl, app)

    dest.write_text(converted, encoding="utf-8")
    print(f"  Converted schema: {schema_file.name} -> {dest}")
    return True


def export_queries(conn: sqlite3.Connection, app: str, output_dir: Path, min_tables: int):
    """从 wtune_stmts 表导出 SQL 查询"""
    query_dir = output_dir / "query"
    query_dir.mkdir(parents=True, exist_ok=True)

    cursor = conn.execute(
        "SELECT stmt_id, stmt_raw_sql FROM wtune_stmts WHERE stmt_app_name = ? ORDER BY stmt_id",
        (app,)
    )
    rows = cursor.fetchall()

    exported = 0
    skipped = 0
    for stmt_id, raw_sql in rows:
        if raw_sql is None or raw_sql.strip() == "":
            skipped += 1
            continue
        if min_tables > 1 and is_simple_query(raw_sql):
            skipped += 1
            continue

        sql_file = query_dir / f"q{stmt_id}.sql"
        sql_file.write_text(raw_sql.strip() + "\n", encoding="utf-8")
        exported += 1

    print(f"  Exported {exported} queries, skipped {skipped} (simple/empty) for {app}")
    return exported


def main():
    parser = argparse.ArgumentParser(description="Migrate WeTune data to AutoRewriter")
    parser.add_argument("--wetune-root", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    parser.add_argument("--apps", nargs="+", default=None, help="Apps to migrate (default: all)")
    parser.add_argument("--min-tables", type=int, default=2,
                        help="Minimum tables in query (1=all, 2=skip single-table)")
    args = parser.parse_args()

    db_path = args.wetune_root / "wtune_data" / "wtune.db"
    if not db_path.exists():
        print(f"ERROR: wtune.db not found at {db_path}", file=sys.stderr)
        sys.exit(1)

    conn = sqlite3.connect(str(db_path))

    if args.apps:
        all_apps = args.apps
    else:
        cursor = conn.execute(
            "SELECT DISTINCT stmt_app_name FROM wtune_stmts WHERE stmt_app_name != 'broadleaf_tmp' ORDER BY stmt_app_name"
        )
        all_apps = [row[0] for row in cursor.fetchall()]

    print(f"Migrating {len(all_apps)} apps: {all_apps}")

    for app in all_apps:
        app_dir = args.output_root / f"wetune_{app}"
        print(f"\nProcessing {app} -> {app_dir}")

        ok = export_schema(args.wetune_root, app, app_dir)
        if not ok:
            print(f"  Skipping {app} (no schema)")
            continue

        count = export_queries(conn, app, app_dir, args.min_tables)
        if count == 0:
            print(f"  [WARN] No queries exported for {app}")

    conn.close()
    print("\nMigration complete.")


if __name__ == "__main__":
    main()


def export_queries(conn: sqlite3.Connection, app: str, output_dir: Path, min_tables: int):
    """从 wtune_stmts 表导出 SQL 查询"""
    query_dir = output_dir / "query"
    query_dir.mkdir(parents=True, exist_ok=True)

    cursor = conn.execute(
        "SELECT stmt_id, stmt_raw_sql FROM wtune_stmts WHERE stmt_app_name = ? ORDER BY stmt_id",
        (app,)
    )
    rows = cursor.fetchall()

    exported = 0
    skipped = 0
    for stmt_id, raw_sql in rows:
        if raw_sql is None or raw_sql.strip() == "":
            skipped += 1
            continue
        if min_tables > 1 and is_simple_query(raw_sql):
            skipped += 1
            continue

        sql_file = query_dir / f"q{stmt_id}.sql"
        sql_file.write_text(raw_sql.strip() + "\n", encoding="utf-8")
        exported += 1

    print(f"  Exported {exported} queries, skipped {skipped} (simple/empty) for {app}")
    return exported

def main():
    parser = argparse.ArgumentParser(description="Migrate WeTune data to AutoRewriter")
    parser.add_argument("--wetune-root", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    parser.add_argument("--apps", nargs="+", default=None, help="Apps to migrate (default: all)")
    parser.add_argument("--min-tables", type=int, default=2,
                        help="Minimum tables in query (1=all, 2=skip single-table)")
    args = parser.parse_args()

    db_path = args.wetune_root / "wtune_data" / "wtune.db"
    if not db_path.exists():
        print(f"ERROR: wtune.db not found at {db_path}", file=sys.stderr)
        sys.exit(1)

    conn = sqlite3.connect(str(db_path))

    # 获取所有 app 列表
    if args.apps:
        all_apps = args.apps
    else:
        cursor = conn.execute(
            "SELECT DISTINCT stmt_app_name FROM wtune_stmts WHERE stmt_app_name != 'broadleaf_tmp' ORDER BY stmt_app_name"
        )
        all_apps = [row[0] for row in cursor.fetchall()]

    print(f"Migrating {len(all_apps)} apps: {all_apps}")

    for app in all_apps:
        app_dir = args.output_root / f"wetune_{app}"
        print(f"\nProcessing {app} -> {app_dir}")

        ok = export_schema(args.wetune_root, app, app_dir)
        if not ok:
            print(f"  Skipping {app} (no schema)")
            continue

        count = export_queries(conn, app, app_dir, args.min_tables)
        if count == 0:
            print(f"  [WARN] No queries exported for {app}")

    conn.close()
    print("\nMigration complete.")

if __name__ == "__main__":
    main()
