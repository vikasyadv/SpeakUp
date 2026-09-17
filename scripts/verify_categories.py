import sys
from cat_tech_society import TECHNOLOGY, SOCIETY
from cat_mindset_life import MINDSET, LIFE
from cat_career_edu import CAREER, EDUCATION
from cat_future_env import FUTURE, ENVIRONMENT
from cat_science_people_creativity import SCIENCE, PEOPLE, CREATIVITY

categories = {
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

print("Raw counts:")
for cat_id, (name, lst, target) in categories.items():
    print(f"Cat {cat_id} ({name}): count={len(lst)}, target={target}")
