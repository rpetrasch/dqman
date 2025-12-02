# Data Quality Tool - Implementation Plan (Phase 2)

## Goal Description
Enhance the Data Quality Tool with a professional UI layout (Sidebar), flexible rule execution (Predicates), and Data Integration capabilities using Apache Camel.

## User Review Required
- **Camel Integration**: Confirm if Camel routes should be defined in code (Java DSL) or stored in DB (XML/YAML) and loaded dynamically. *Assumption: Started with Java DSL for simplicity, but user mentioned "Data Integrations" menu, implying some UI management.*
- **Logo**: I will use a placeholder text/icon for "DQ Tool" unless an image asset is provided.

## Proposed Changes

### Frontend (Angular)
- **Layout**:
    - Implement `MainLayoutComponent` using `mat-sidenav`.
    - Add Sidebar Menu: DQ Rules, DQ Projects, Data Integration, Alerts, Reports, Kestra Flows.
    - Update `AppComponent` to use the new layout.
- **Styling**:
    - Add "DQ Tool" title and logo to the toolbar.
    - Improve overall aesthetics (spacing, colors).
- **New Components**:
    - Create placeholder components for `DqProjects`, `DataIntegration`, `Alerts`, `Reports`, `KestraFlows`.

### Backend (Spring Boot)
- **Apache Camel**:
    - Add `camel-spring-boot-starter`.
    - Create `CamelConfig` to set up Camel Context.
    - Implement example route for Data Integration.
- **Flexible DQ Rules**:
    - Refactor [DqRule](file:///Users/rolandpetrasch/Projects/dqman/_4_6_dq_tool/src/main/java/org/dqman/model/DqRule.java#9-22) usage.
    - Implement a `RuleEvaluator` service that uses `Predicate` logic.
    - Support types: `SQL` (execute query), `REGEX` (match pattern), `MANUAL` (placeholder).

## Verification Plan
### Automated Tests
- **Backend**: Test Camel context startup. Test RuleEvaluator with different rule types.
- **Frontend**: Verify navigation between new menu items.

### Manual Verification
- **UI**: Check Sidebar responsiveness and "DQ Tool" branding.
- **Camel**: Verify a simple file move or log route works.
- **Rules**: Create a Regex rule and verify it validates data correctly (mocked).
