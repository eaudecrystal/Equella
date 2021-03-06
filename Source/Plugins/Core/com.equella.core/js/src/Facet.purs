module Facet where

import Prelude

import CheckList (checkList)
import Control.Monad.Eff (Eff)
import Control.Monad.Eff.Class (liftEff)
import Control.Monad.Eff.Console (log)
import Control.Monad.Eff.Uncurried (mkEffFn2)
import Control.Monad.State (modify)
import Control.Monad.Trans.Class (lift)
import Data.Argonaut (class DecodeJson, decodeJson, (.?))
import Data.Either (either)
import Data.Maybe (Maybe(..))
import Data.Set (Set, member)
import Data.Tuple (Tuple(..))
import Debug.Trace (traceAnyA)
import Dispatcher (dispatch)
import Dispatcher.React (ReactProps(ReactProps), createLifecycleComponent, didMount, getProps, modifyState)
import EQUELLA.Environment (baseUrl)
import MaterialUI.Icon (icon_)
import MaterialUI.ListItemText (primary, secondary)
import MaterialUI.Properties (onChange)
import MaterialUI.SwitchBase (checked)
import Network.HTTP.Affjax (get)
import QueryString (queryString)
import React (ReactElement, createFactory)
import React.DOM (text)
import React.DOM as D
import SearchFilters (filterSection)
import Settings.UISettings (FacetSetting(..))

newtype FacetResult = FacetResult {term::String, count::Int}
newtype FacetResults = FacetResults (Array FacetResult)

instance frDecode :: DecodeJson FacetResult where
  decodeJson v = do
    o <- decodeJson v
    term <- o .? "term"
    count <- o .? "count"
    pure $ FacetResult {term,count}

instance frsDecode :: DecodeJson FacetResults where
  decodeJson v = do
    o <- decodeJson v
    results <- o .? "results"
    pure $ FacetResults results

type Props eff = {
  facet :: FacetSetting,
  onClickTerm :: String -> Eff eff Unit,
  selectedTerms :: Set String,
  query :: Array (Tuple String String)
}

type State = {
  searching :: Boolean,
  searchResults :: Maybe FacetResults
}

data Command = Search | UpdatedProps (Array (Tuple String String))

initialState :: State
initialState = {searching:false, searchResults:Nothing}

facetDisplay :: forall eff. Props eff -> ReactElement
facetDisplay = createFactory (createLifecycleComponent (do
    didMount Search
    modify _ { componentDidUpdate = \c {query} _ -> dispatch eval c (UpdatedProps query) }
    ) initialState render eval)
  where
  render {searchResults} (ReactProps {facet:(FacetSetting {name}), onClickTerm,selectedTerms}) = 
    filterSection {name, icon: icon_ [text "view_list"] } [
      renderResults searchResults
    ]
    where
    renderResults (Just (FacetResults results)) = checkList {entries: result <$> results}
      where 
      result (FacetResult {term,count}) = {
        checkProps : [checked $ member term selectedTerms, onChange $ (mkEffFn2 \e c -> onClickTerm term)],
        textProps : [primary term, secondary $ show count]
      }
    renderResults _ = D.div' []

  searchWith query = do
    modifyState _ {searching=true}
    {facet:(FacetSetting {path})} <- getProps
    result <- lift $ get $ baseUrl <> "api/search/facet?" <> (queryString $ [Tuple "nodes" path] <> query)
    either (lift <<< liftEff <<< log) (\r -> modifyState _ {searchResults=Just r}) $ decodeJson result.response

  eval Search = do
    {query} <- getProps
    searchWith query
  eval (UpdatedProps oldQuery) = do
    {query} <- getProps
    traceAnyA {query, oldQuery}
    if oldQuery /= query then searchWith query else pure unit
