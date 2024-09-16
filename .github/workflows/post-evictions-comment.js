const postEvictionsComment = async (github, context) => {
    const fs = require('fs');
    const reportContent = fs.readFileSync('unique_evictions.txt', 'utf8');

    const comments = await github.rest.issues.listComments({
        issue_number: context.issue.number,
        owner: context.repo.owner,
        repo: context.repo.repo
    });

    const commentMarker = 'Suspected binary incompatible evictions across all projects (summary)';
    const existingComment = comments.data.find(comment => comment.body.includes(commentMarker));

    if (existingComment) {
        await github.rest.issues.updateComment({
            comment_id: existingComment.id,
            owner: context.repo.owner,
            repo: context.repo.repo,
            body: reportContent
        });
    }
    else {
        github.rest.issues.createComment({
            issue_number: context.issue.number,
            owner: context.repo.owner,
            repo: context.repo.repo,
            body: reportContent
        });
    }
}

module.exports = postEvictionsComment;
