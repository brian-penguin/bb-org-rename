# Github Org Rename

Goal: 
We want to rename all of our default branches from master -> main. 

Why: 
- https://constable.io/announcements/3634-finalizing-our-rename-from-master-to-main-on-public-repositories
- https://thoughtbot.com/blog/it-s-time-for-a-new-branch
- https://github.com/thoughtbot/handbook/issues/1771
- https://www.hanselman.com/blog/easily-rename-your-git-default-branch-from-master-to-main


Strategy: 
Break up each action into a series of scripts 

1. Get a User auth token for github. Test that this token works
1. Get all the repos for an org. We need the name and the default branch
1. Filter all repos which have a default branch of "master"
1. Create new branch "main" from SHA on master
1. Update Repo with new default branch
1. Delete master branch

