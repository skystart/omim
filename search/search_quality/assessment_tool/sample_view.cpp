#include "search/search_quality/assessment_tool/sample_view.hpp"

#include "qt/qt_common/spinner.hpp"

#include "map/bookmark_manager.hpp"
#include "map/framework.hpp"
#include "map/user_mark.hpp"

#include "search/result.hpp"
#include "search/search_quality/assessment_tool/helpers.hpp"
#include "search/search_quality/assessment_tool/result_view.hpp"
#include "search/search_quality/assessment_tool/results_view.hpp"
#include "search/search_quality/sample.hpp"

#include <QtGui/QStandardItem>
#include <QtGui/QStandardItemModel>
#include <QtWidgets/QComboBox>
#include <QtWidgets/QGroupBox>
#include <QtWidgets/QHBoxLayout>
#include <QtWidgets/QHeaderView>
#include <QtWidgets/QLabel>
#include <QtWidgets/QLineEdit>
#include <QtWidgets/QListWidget>
#include <QtWidgets/QMenu>
#include <QtWidgets/QPushButton>
#include <QtWidgets/QVBoxLayout>

namespace
{
template <typename Layout>
Layout * BuildSubLayout(QLayout & mainLayout, QWidget & parent, QWidget ** box)
{
  *box = new QWidget(&parent);
  auto * subLayout = BuildLayoutWithoutMargins<Layout>(*box /* parent */);
  (*box)->setLayout(subLayout);
  mainLayout.addWidget(*box);
  return subLayout;
}

template <typename Layout>
Layout * BuildSubLayout(QLayout & mainLayout, QWidget & parent)
{
  QWidget * box = nullptr;
  return BuildSubLayout<Layout>(mainLayout, parent, &box);
}

void SetVerticalStretch(QWidget & widget, int stretch)
{
  auto policy = widget.sizePolicy();
  policy.setVerticalStretch(stretch);
  widget.setSizePolicy(policy);
}
}  // namespace

SampleView::SampleView(QWidget * parent, Framework & framework)
  : QWidget(parent), m_framework(framework)
{
  auto * mainLayout = BuildLayout<QVBoxLayout>(this /* parent */);

  // When the dock for SampleView is attached to the right side of the
  // screen, we don't need left margin, because of zoom in/zoom out
  // slider. In other cases, it's better to keep left margin as is.
  m_defaultMargins = mainLayout->contentsMargins();
  m_rightAreaMargins = m_defaultMargins;
  m_rightAreaMargins.setLeft(0);

  {
    m_query = new QLabel(this /* parent */);
    m_query->setToolTip(tr("Query text"));
    m_query->setWordWrap(true);
    m_query->hide();
    mainLayout->addWidget(m_query);
  }

  {
    m_langs = new QLabel(this /* parent */);
    m_langs->setToolTip(tr("Query input language"));
    m_langs->hide();
    mainLayout->addWidget(m_langs);
  }

  {
    auto * layout = BuildSubLayout<QHBoxLayout>(*mainLayout, *this /* parent */);

    m_showViewport = new QPushButton(tr("Show viewport"), this /* parent */);
    connect(m_showViewport, &QPushButton::clicked, [this]() { emit OnShowViewportClicked(); });
    layout->addWidget(m_showViewport);

    m_showPosition = new QPushButton(tr("Show position"), this /* parent */);
    connect(m_showPosition, &QPushButton::clicked, [this]() { emit OnShowPositionClicked(); });
    layout->addWidget(m_showPosition);
  }

  {
    auto * layout =
        BuildSubLayout<QVBoxLayout>(*mainLayout, *this /* parent */, &m_relatedQueriesBox);
    SetVerticalStretch(*m_relatedQueriesBox, 1 /* stretch */);
    layout->addWidget(new QLabel(tr("Related queries")));

    m_relatedQueries = new QListWidget();
    layout->addWidget(m_relatedQueries);
  }

  {
    auto * layout =
        BuildSubLayout<QVBoxLayout>(*mainLayout, *this /* parent */, &m_foundResultsBox);
    SetVerticalStretch(*m_foundResultsBox, 4 /* stretch */);

    {
      auto * subLayout = BuildSubLayout<QHBoxLayout>(*layout, *this /* parent */);
      subLayout->addWidget(new QLabel(tr("Found results")));

      m_spinner = new Spinner();
      subLayout->addWidget(&m_spinner->AsWidget());
    }

    m_foundResults = new ResultsView(*this /* parent */);
    layout->addWidget(m_foundResults);
  }

  {
    auto * layout =
        BuildSubLayout<QVBoxLayout>(*mainLayout, *this /* parent */, &m_nonFoundResultsBox);
    SetVerticalStretch(*m_nonFoundResultsBox, 2 /* stretch */);

    layout->addWidget(new QLabel(tr("Non found results")));

    m_nonFoundResults = new ResultsView(*this /* parent */);
    m_nonFoundResults->setContextMenuPolicy(Qt::CustomContextMenu);
    connect(m_nonFoundResults, &ResultsView::customContextMenuRequested, [&](QPoint pos) {
      pos = m_nonFoundResults->mapToGlobal(pos);

      auto const items = m_nonFoundResults->selectedItems();
      for (auto const * item : items)
      {
        int const row = m_nonFoundResults->row(item);

        QMenu menu;
        auto const * action = menu.addAction("Remove result");
        connect(action, &QAction::triggered, [this, row]() { OnRemoveNonFoundResult(row); });

        menu.exec(pos);
      }
    });
    layout->addWidget(m_nonFoundResults);
  }

  setLayout(mainLayout);

  Clear();
}

void SampleView::SetContents(search::Sample const & sample, bool positionAvailable)
{
  if (!sample.m_query.empty())
  {
    m_query->setText(ToQString(sample.m_query));
    m_query->show();
  }
  if (!sample.m_locale.empty())
  {
    m_langs->setText(ToQString(sample.m_locale));
    m_langs->show();
  }
  m_showViewport->setEnabled(true);
  m_showPosition->setEnabled(positionAvailable);

  m_relatedQueries->clear();
  for (auto const & query : sample.m_relatedQueries)
    m_relatedQueries->addItem(ToQString(query));
  if (m_relatedQueries->count() != 0)
    m_relatedQueriesBox->show();

  ClearAllResults();
}

void SampleView::OnSearchStarted() { m_spinner->Show(); }

void SampleView::OnSearchCompleted() { m_spinner->Hide(); }

void SampleView::ShowFoundResults(search::Results::ConstIter begin, search::Results::ConstIter end)
{
  for (auto it = begin; it != end; ++it)
    m_foundResults->Add(*it /* result */);
}

void SampleView::ShowNonFoundResults(std::vector<search::Sample::Result> const & results,
                                     std::vector<Edits::Entry> const & entries)
{
  CHECK_EQUAL(results.size(), entries.size(), ());

  auto & bookmarkManager = m_framework.GetBookmarkManager();
  UserMarkControllerGuard guard(bookmarkManager, UserMarkType::SEARCH_MARK);
  guard.m_controller.SetIsVisible(true);
  guard.m_controller.SetIsDrawable(true);

  bool allDeleted = true;
  for (size_t i = 0; i < results.size(); ++i)
  {
    m_nonFoundResults->Add(results[i], entries[i]);
    if (!entries[i].m_deleted)
      allDeleted = false;
  }
  if (!allDeleted)
    m_nonFoundResultsBox->show();
}

void SampleView::ShowFoundResultsMarks(search::Results::ConstIter begin, search::Results::ConstIter end)
{
  m_framework.FillSearchResultsMarks(begin, end);
}

void SampleView::ShowNonFoundResultsMarks(std::vector<search::Sample::Result> const & results,
                                          std::vector<Edits::Entry> const & entries)

{
  CHECK_EQUAL(results.size(), entries.size(), ());

  auto & bookmarkManager = m_framework.GetBookmarkManager();
  UserMarkControllerGuard guard(bookmarkManager, UserMarkType::SEARCH_MARK);
  guard.m_controller.SetIsVisible(true);
  guard.m_controller.SetIsDrawable(true);

  for (size_t i = 0; i < results.size(); ++i)
  {
    auto const & result = results[i];
    auto const & entry = entries[i];
    if (entry.m_deleted)
      continue;

    SearchMarkPoint * mark =
        static_cast<SearchMarkPoint *>(guard.m_controller.CreateUserMark(result.m_pos));
    mark->SetCustomSymbol("non-found-search-result");
  }
}

void SampleView::ClearSearchResultMarks() { m_framework.ClearSearchResultsMarks(); }

void SampleView::ClearAllResults()
{
  m_foundResults->Clear();
  m_nonFoundResults->Clear();
  m_nonFoundResultsBox->hide();
  ClearSearchResultMarks();
}

void SampleView::SetEdits(Edits & resultsEdits, Edits & nonFoundResultsEdits)
{
  SetEdits(*m_foundResults, resultsEdits);
  SetEdits(*m_nonFoundResults, nonFoundResultsEdits);
  m_nonFoundResultsEdits = &nonFoundResultsEdits;
}

void SampleView::Clear()
{
  m_query->hide();
  m_langs->hide();

  m_showViewport->setEnabled(false);
  m_showPosition->setEnabled(false);

  m_relatedQueriesBox->hide();

  ClearAllResults();
  OnSearchCompleted();
}

void SampleView::OnLocationChanged(Qt::DockWidgetArea area)
{
  if (area == Qt::RightDockWidgetArea)
    layout()->setContentsMargins(m_rightAreaMargins);
  else
    layout()->setContentsMargins(m_defaultMargins);
}

void SampleView::SetEdits(ResultsView & results, Edits & edits)
{
  size_t const numRelevances = edits.GetRelevances().size();
  CHECK_EQUAL(results.Size(), numRelevances, ());
  for (size_t i = 0; i < numRelevances; ++i)
    results.Get(i).SetEditor(Edits::RelevanceEditor(edits, i));
}

void SampleView::OnRemoveNonFoundResult(int row) { m_nonFoundResultsEdits->Delete(row); }
