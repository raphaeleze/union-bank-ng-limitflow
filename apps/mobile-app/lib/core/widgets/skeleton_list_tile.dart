import 'package:flutter/material.dart';

import 'info_card.dart';
import 'skeleton_box.dart';

/// Placeholder for a row-shaped list item (notification, request history
/// entry, ...) — an avatar-sized box plus two lines of text, matching the
/// shape most of this app's list rows share.
class SkeletonListTile extends StatelessWidget {
  const SkeletonListTile({super.key});

  @override
  Widget build(BuildContext context) {
    return const InfoCard(
      child: Row(
        children: [
          SkeletonBox(width: 40, height: 40, borderRadius: 20),
          SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SkeletonBox(width: 120, height: 14),
                SizedBox(height: 8),
                SkeletonBox(height: 12),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// A vertical list of [SkeletonListTile]s with the same padding/spacing the
/// real lists use.
class SkeletonList extends StatelessWidget {
  const SkeletonList({super.key, this.itemCount = 4});

  final int itemCount;

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(20),
      itemCount: itemCount,
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (context, index) => const SkeletonListTile(),
    );
  }
}
