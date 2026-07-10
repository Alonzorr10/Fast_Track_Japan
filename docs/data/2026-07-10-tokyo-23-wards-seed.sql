-- Seed all 23 special wards of Tokyo into garbage_wards.
--
-- Idempotent: `on conflict do nothing` skips wards that are already present
-- (the initial garbage-schema migration seeded Setagaya, so re-running this
-- file after that migration is safe).
--
-- IMPORTANT: this ONLY seeds ward *names* so the ward dropdown populates.
-- No garbage_areas (chōme) rows and no garbage_schedules rows are inserted.
-- Users who select a ward that has not had chōme + schedule data seeded will
-- see an empty district dropdown and, if they somehow get past that, no
-- upcoming collections. This is intentional — accurate collection data must
-- be sourced ward-by-ward from official/open-data portals; see
-- `docs/data/tokyo-ward-schedule-sourcing.md`.
--
-- Ward codes use short lowercase slugs to match the existing Setagaya row
-- (do not switch to JIS municipal codes without also migrating existing
-- garbage_areas.wardCode foreign-key references).

insert into "garbage_wards" ("code","nameJa","nameEn") values
  ('chiyoda',    '千代田区',   'Chiyoda'),
  ('chuo',       '中央区',     'Chuo'),
  ('minato',     '港区',       'Minato'),
  ('shinjuku',   '新宿区',     'Shinjuku'),
  ('bunkyo',     '文京区',     'Bunkyo'),
  ('taito',      '台東区',     'Taito'),
  ('sumida',     '墨田区',     'Sumida'),
  ('koto',       '江東区',     'Koto'),
  ('shinagawa',  '品川区',     'Shinagawa'),
  ('meguro',     '目黒区',     'Meguro'),
  ('ota',        '大田区',     'Ota'),
  ('setagaya',   '世田谷区',   'Setagaya'),
  ('shibuya',    '渋谷区',     'Shibuya'),
  ('nakano',     '中野区',     'Nakano'),
  ('suginami',   '杉並区',     'Suginami'),
  ('toshima',    '豊島区',     'Toshima'),
  ('kita',       '北区',       'Kita'),
  ('arakawa',    '荒川区',     'Arakawa'),
  ('itabashi',   '板橋区',     'Itabashi'),
  ('nerima',     '練馬区',     'Nerima'),
  ('adachi',     '足立区',     'Adachi'),
  ('katsushika', '葛飾区',     'Katsushika'),
  ('edogawa',    '江戸川区',   'Edogawa')
on conflict ("code") do nothing;
