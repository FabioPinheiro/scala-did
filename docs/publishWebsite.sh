# #!/bin/sh
# scriptdir="$(dirname "$0")"
# currentBranch="$(git branch --show-current)"
# inputAPI="$scriptdir/target/scaladoc/unidoc"
# outdir="$scriptdir/target/gppages"

# [[ -z $(git status -uall --porcelain) ]] && {
#   (cd "$scriptdir/.." ; sbt -mem 2048 -J-Xmx5120m "docAll; siteAll") # clean;
#   mkdir -p $outdir
#   cp -r "$scriptdir/target/site/did-doc/." "$outdir/" &&
#   cp -r "$scriptdir/target/scaladoc/unidoc" "$outdir/api" &&
#   git branch gh-pages &&
#   git checkout gh-pages &&
#   git pull &&
#   git add --force "$outdir" &&
#   git commit -m "gh-pages update at $(date '+%Y%m%d-%H%M%S')" &&
#   git push --set-upstream origin gh-pages -f &&
#   git checkout $currentBranch &&
#   git branch -d gh-pages # delete branch locally
# } || echo "This branch is NOT clean."

# #Now check https://fabiopinheiro.github.io/scala-did/
# #aka https://doc.did.fmgp.app/