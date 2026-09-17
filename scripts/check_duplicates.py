from cat_tech_society import TECHNOLOGY, SOCIETY
from cat_mindset_life import MINDSET, LIFE
from cat_career_edu import CAREER, EDUCATION
from cat_future_env import FUTURE, ENVIRONMENT
from cat_science_people_creativity import SCIENCE, PEOPLE, CREATIVITY

raw_categories = {
    1: ("Technology", TECHNOLOGY, 182),
    2: ("Society", SOCIETY, 182),
    3: ("Mindset", MINDSET, 182),
    4: ("Life", LIFE, 182),
    5: ("Career", CAREER, 182),
    6: ("Education", EDUCATION, 182),
    7: ("Future", FUTURE, 182),
    8: ("Environment", ENVIRONMENT, 182),
    9: ("Science", SCIENCE, 182),
    10: ("People", PEOPLE, 181),
    11: ("Creativity", CREATIVITY, 181),
}

seen = {}
duplicates = []

for cat_id, (name, lst, target) in raw_categories.items():
    for item in lst:
        key = item.strip().lower()
        if key in seen:
            duplicates.append((item, cat_id, name, seen[key]))
        else:
            seen[key] = (cat_id, name)

print(f"Duplicates across categories: {len(duplicates)}")
for dup in duplicates:
    print(f"  '{dup[0]}' in Cat {dup[1]} ({dup[2]}), first seen in Cat {dup[3][0]} ({dup[3][1]})")
