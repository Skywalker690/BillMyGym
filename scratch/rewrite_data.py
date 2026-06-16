import re

with open('backend/src/main/resources/data.sql', 'r', encoding='utf-8') as f:
    sql = f.read()

# 1. Update organizations subscription plans
sql = sql.replace(
    "(ARRAY['BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM'])[org_id]",
    "(ARRAY['BILLMYGYM', 'BASE', 'BILLMYGYM', 'BASE', 'BILLMYGYM', 'BASE', 'BILLMYGYM', 'BASE', 'BILLMYGYM', 'BASE'])[org_id]"
)

# 2. Update organizations statuses
sql = sql.replace(
    "(ARRAY['ACTIVE', 'PAUSED', 'TRIAL', 'PAUSED', 'ACTIVE', 'PAUSED', 'ACTIVE', 'PAUSED', 'ACTIVE', 'ACTIVE'])[org_id]",
    "(ARRAY['ACTIVE', 'PAUSED', 'TRIAL', 'PAUSED', 'ACTIVE', 'DEACTIVATED', 'TRIAL', 'PAUSED', 'ACTIVE', 'ACTIVE'])[org_id]"
)

# 3. Increase number of members to 100-200 per organization
sql = sql.replace(
    "CROSS JOIN generate_series(1, 10) AS slot_id;",
    "CROSS JOIN LATERAL generate_series(1, 100 + (org_id * 10)) AS slot_id;"
)

# 4. Fix ID collision logic globally
sql = sql.replace("((org_id - 1) * 10)", "((org_id - 1) * 300)")

# 5. Fix array indices for names so we don't get "index out of bounds" when slot_id > 10
sql = sql.replace(
    "(ARRAY['Aarav', 'Diya', 'Rahul', 'Sneha', 'Vivek', 'Nisha', 'Karthik', 'Amina', 'Joel', 'Meera'])[slot_id]",
    "(ARRAY['Aarav', 'Diya', 'Rahul', 'Sneha', 'Vivek', 'Nisha', 'Karthik', 'Amina', 'Joel', 'Meera'])[((slot_id - 1) % 10) + 1]"
)

# 6. Adjust modulo logic so deactivations and logic still apply evenly
sql = sql.replace("WHEN slot_id IN (4, 5)", "WHEN slot_id % 10 IN (4, 5)")
sql = sql.replace("WHEN slot_id = 4", "WHEN slot_id % 10 = 4")
sql = sql.replace("WHEN slot_id = 5", "WHEN slot_id % 10 = 5")

# Fix member created_at interval 
sql = sql.replace(
    "(((org_id * 4) + slot_id + 20) || ' days')::interval",
    "(((org_id * 4) + (slot_id % 20) + 20) || ' days')::interval"
)

# Fix member_memberships slot derivation
sql = sql.replace(
    "CROSS JOIN LATERAL (SELECT ((member.id - 1) % 10) + 1 AS slot_id) slot",
    "CROSS JOIN LATERAL (SELECT ((member.id - 1) % 300) + 1 AS slot_id) slot"
)

# Member payments ID generation collision fix
sql = sql.replace(
    "100 + ((membership.organization_id - 1) * 2) + CASE WHEN slot.slot_id = 1 THEN 1 ELSE 2 END,",
    "5000 + ((membership.organization_id - 1) * 600) + slot.slot_id,"
)
sql = sql.replace(
    "WHERE slot.slot_id IN (1, 9);",
    "WHERE slot.slot_id % 10 IN (1, 9);"
)

with open('backend/src/main/resources/data.sql', 'w', encoding='utf-8') as f:
    f.write(sql)
print("Updated data.sql")
