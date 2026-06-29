import { readFile } from 'node:fs/promises';
import path from 'node:path';
import ts from 'typescript';

const root = process.cwd();
const registryPath = path.join(root, 'libs/widgets/src/lib/widget-registry.ts');
const fallbackPath = path.join(
  root,
  'libs/shared-assets/public/assets/fallback/public-bootstrap-fallback.fr.json',
);

function propertyNameText(name) {
  if (ts.isIdentifier(name) || ts.isStringLiteral(name) || ts.isNumericLiteral(name)) {
    return name.text;
  }
  return undefined;
}

function collectRegistryKeys(sourceText) {
  const source = ts.createSourceFile(
    registryPath,
    sourceText,
    ts.ScriptTarget.Latest,
    true,
    ts.ScriptKind.TS,
  );
  const keys = new Set();

  function visit(node) {
    if (
      ts.isVariableDeclaration(node) &&
      ts.isIdentifier(node.name) &&
      node.name.text === 'WIDGET_REGISTRY' &&
      node.initializer &&
      ts.isObjectLiteralExpression(node.initializer)
    ) {
      for (const property of node.initializer.properties) {
        if (ts.isShorthandPropertyAssignment(property)) {
          keys.add(property.name.text);
        } else if (ts.isPropertyAssignment(property)) {
          const key = propertyNameText(property.name);
          if (key) {
            keys.add(key);
          }
        }
      }
    }
    ts.forEachChild(node, visit);
  }

  visit(source);
  return keys;
}

function collectLayoutWidgetIds(pagePayload) {
  const ids = new Set();
  for (const row of pagePayload?.content?.layout?.rows ?? []) {
    for (const column of row.columns ?? []) {
      for (const widgetId of column.widgets ?? []) {
        ids.add(widgetId);
      }
    }
  }
  return ids;
}

function difference(left, right) {
  return [...left].filter(value => !right.has(value)).sort();
}

const registryKeys = collectRegistryKeys(await readFile(registryPath, 'utf8'));
const fallback = JSON.parse(await readFile(fallbackPath, 'utf8'));
const pagePayload = fallback.pagePayload;
const widgets = pagePayload?.content?.widgets ?? {};
const declaredWidgetIds = new Set(Object.keys(widgets));
const layoutWidgetIds = collectLayoutWidgetIds(pagePayload);
const widgetTypes = new Set(
  Object.values(widgets)
    .map(widget => widget?.type)
    .filter(Boolean),
);

const missingTypes = difference(widgetTypes, registryKeys);
const missingWidgetIds = difference(layoutWidgetIds, declaredWidgetIds);
const unreferencedWidgetIds = difference(declaredWidgetIds, layoutWidgetIds);

console.log(
  `widget registry contract (${registryKeys.size} registry entries, ${declaredWidgetIds.size} fallback widgets)`,
);
console.log(`- fallback widget types: ${widgetTypes.size}`);
console.log(`- missing registry types: ${missingTypes.length}`);
console.log(`- layout ids without widget config: ${missingWidgetIds.length}`);
console.log(`- widget configs not referenced by layout: ${unreferencedWidgetIds.length}`);

if (missingTypes.length || missingWidgetIds.length || unreferencedWidgetIds.length) {
  if (missingTypes.length) {
    console.error(`missing registry types:\n${missingTypes.map(type => `  - ${type}`).join('\n')}`);
  }
  if (missingWidgetIds.length) {
    console.error(
      `layout ids without widget config:\n${missingWidgetIds.map(id => `  - ${id}`).join('\n')}`,
    );
  }
  if (unreferencedWidgetIds.length) {
    console.error(
      `widget configs not referenced by layout:\n${unreferencedWidgetIds.map(id => `  - ${id}`).join('\n')}`,
    );
  }
  process.exitCode = 1;
}
