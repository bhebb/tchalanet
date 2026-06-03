-- Seed theme presets — 12 Material Design 3 themes + tchalanet brand default
-- tchalanet (sort_order=0, is_default=true) is used for public runtime when no tenant theme is set.
-- All other themes are selectable by tenant admins via POST /admin/theme/preset.

INSERT INTO theme_preset (id, code, vendor, label_key, description, sort_order, active, is_default, config)
VALUES
  (
    '00000000-0000-0000-0000-000000000401'::uuid,
    'tchalanet',
    'tchalanet',
    'theme.presets.tchalanet',
    'Tchalanet brand theme — Caribbean teal, used as public default',
    0, true, true,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#006874","color.secondary":"#4A6267","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#4FD8E8","color.secondary":"#ACCFD5","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000402'::uuid,
    'm3-purple',
    'material',
    'theme.presets.materialPurple',
    'Material 3 baseline purple',
    1, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#6750A4","color.secondary":"#625B71","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#D0BCFF","color.secondary":"#CCC2DC","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000403'::uuid,
    'm3-blue',
    'material',
    'theme.presets.materialBlue',
    'Material 3 blue',
    2, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#0061A4","color.secondary":"#535F70","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#9ECAFF","color.secondary":"#BBC7DB","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000404'::uuid,
    'm3-green',
    'material',
    'theme.presets.materialGreen',
    'Material 3 green',
    3, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#006E1C","color.secondary":"#52634F","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#72DD68","color.secondary":"#B9CCB2","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000405'::uuid,
    'm3-red',
    'material',
    'theme.presets.materialRed',
    'Material 3 red',
    4, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#BA1A1A","color.secondary":"#775652","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#FFB4AB","color.secondary":"#E7BDB8","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000406'::uuid,
    'm3-orange',
    'material',
    'theme.presets.materialOrange',
    'Material 3 orange',
    5, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#8B5000","color.secondary":"#715A41","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#FFB870","color.secondary":"#DFBEA2","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000407'::uuid,
    'm3-teal',
    'material',
    'theme.presets.materialTeal',
    'Material 3 teal',
    6, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#006A60","color.secondary":"#4A635F","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#4FDBD0","color.secondary":"#AACFCA","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000408'::uuid,
    'm3-pink',
    'material',
    'theme.presets.materialPink',
    'Material 3 pink',
    7, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#9C4069","color.secondary":"#74565D","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#FFB1C5","color.secondary":"#E4BAC0","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000409'::uuid,
    'm3-indigo',
    'material',
    'theme.presets.materialIndigo',
    'Material 3 indigo',
    8, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#4042A8","color.secondary":"#5B5D72","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#BEC2FF","color.secondary":"#C4C5DD","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000410'::uuid,
    'm3-cyan',
    'material',
    'theme.presets.materialCyan',
    'Material 3 cyan',
    9, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#00687B","color.secondary":"#4A6269","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#48D7F4","color.secondary":"#AACDD5","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000411'::uuid,
    'm3-amber',
    'material',
    'theme.presets.materialAmber',
    'Material 3 amber / gold',
    10, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#6B5D00","color.secondary":"#655F40","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#DFC44A","color.secondary":"#CEC8A6","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  ),
  (
    '00000000-0000-0000-0000-000000000412'::uuid,
    'm3-brown',
    'material',
    'theme.presets.materialBrown',
    'Material 3 brown / earth',
    11, true, false,
    '{"modes":["light","dark"],"defaultMode":"light","tokens":{"light":{"color.primary":"#6D5E51","color.secondary":"#5D5044","color.surface":"#FFFBFE","color.onSurface":"#1C1B1F","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"},"dark":{"color.primary":"#D6C3B7","color.secondary":"#C5B3A8","color.surface":"#141218","color.onSurface":"#E6E0E9","shape.radius.md":"12px","typography.fontFamily":"roboto","density.default":"comfortable"}},"editableTokens":["color.primary","color.secondary","shape.radius.md","typography.fontFamily","density.default"],"allowedFonts":["system","roboto","poppins","inter"]}'
  )
ON CONFLICT (code) DO UPDATE SET
  vendor      = EXCLUDED.vendor,
  label_key   = EXCLUDED.label_key,
  description = EXCLUDED.description,
  sort_order  = EXCLUDED.sort_order,
  active      = EXCLUDED.active,
  is_default  = EXCLUDED.is_default,
  config      = EXCLUDED.config,
  updated_at  = now();
